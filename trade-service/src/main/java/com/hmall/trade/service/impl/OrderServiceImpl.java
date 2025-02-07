package com.hmall.trade.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmall.api.client.CartClient;
import com.hmall.api.client.ItemClient;
import com.hmall.api.dto.ItemDTO;
import com.hmall.api.dto.OrderDetailDTO;
import com.hmall.common.exception.BadRequestException;
import com.hmall.common.utils.UserContext;
import com.hmall.trade.domain.dto.OrderFormDTO;
import com.hmall.trade.domain.po.Order;
import com.hmall.trade.domain.po.OrderDetail;
import com.hmall.trade.mapper.OrderMapper;
import com.hmall.trade.service.IOrderDetailService;
import com.hmall.trade.service.IOrderService;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2023-05-05
 */
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements IOrderService {

    private final ItemClient itemClient;
    private final IOrderDetailService detailService;
    private final CartClient cartClient;
    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);
    @Override
    @GlobalTransactional
    public Long createOrder(OrderFormDTO orderFormDTO) {
        long startTime = System.currentTimeMillis();
        log.info("开始创建订单，订单信息：{}", orderFormDTO);

        try {
            // 1.查询商品信息
            List<OrderDetailDTO> detailDTOS = orderFormDTO.getDetails();
            Map<Long, Integer> itemNumMap = detailDTOS.stream()
                    .collect(Collectors.toMap(OrderDetailDTO::getItemId, OrderDetailDTO::getNum));
            Set<Long> itemIds = itemNumMap.keySet();

            log.info("开始查询商品信息，商品IDs：{}", itemIds);
            List<ItemDTO> items = itemClient.queryItemByIds(itemIds);
            log.info("商品信息查询完成，耗时：{}ms", System.currentTimeMillis() - startTime);

            if (items == null || items.size() < itemIds.size()) {
                throw new BadRequestException("商品不存在");
            }

            // 2.创建订单对象
            Order order = createOrderEntity(items, itemNumMap, orderFormDTO);
            log.info("开始保存订单信息");
            save(order);
            log.info("订单信息保存完成，订单ID：{}", order.getId());

            // 3.保存订单详情
            List<OrderDetail> details = buildDetails(order.getId(), items, itemNumMap);
            log.info("开始保存订单详情信息");
            detailService.saveBatch(details);
            log.info("订单详情保存完成");

            // 4.清理购物车
            log.info("开始清理购物车，商品IDs：{}", itemIds);
            cartClient.deleteCartItemByIds(itemIds);
            log.info("购物车清理完成");

            // 5.扣减库存
            log.info("开始扣减库存");
            try {
                itemClient.deductStock(detailDTOS);
                log.info("库存扣减完成");
            } catch (Exception e) {
                log.error("库存扣减失败", e);
                throw new RuntimeException("库存不足！");
            }

            log.info("订单创建完成，总耗时：{}ms", System.currentTimeMillis() - startTime);
            return order.getId();

        } catch (Exception e) {
            log.error("订单创建失败，耗时：{}ms，异常信息：",
                    System.currentTimeMillis() - startTime, e);
            throw e;
        }
    }

    // 抽取订单实体创建逻辑，提高代码可读性
    private Order createOrderEntity(List<ItemDTO> items,
                                    Map<Long, Integer> itemNumMap,
                                    OrderFormDTO orderFormDTO) {
        Order order = new Order();
        int total = items.stream()
                .mapToInt(item -> item.getPrice() * itemNumMap.get(item.getId()))
                .sum();
        order.setTotalFee(total);
        order.setPaymentType(orderFormDTO.getPaymentType());
        order.setUserId(UserContext.getUser());
        order.setStatus(1);
        return order;
    }

    @Override
    public void markOrderPaySuccess(Long orderId) {
        Order order = new Order();
        order.setId(orderId);
        order.setStatus(2);
        order.setPayTime(LocalDateTime.now());
        updateById(order);
    }

    private List<OrderDetail> buildDetails(Long orderId, List<ItemDTO> items, Map<Long, Integer> numMap) {
        List<OrderDetail> details = new ArrayList<>(items.size());
        for (ItemDTO item : items) {
            OrderDetail detail = new OrderDetail();
            detail.setName(item.getName());
            detail.setSpec(item.getSpec());
            detail.setPrice(item.getPrice());
            detail.setNum(numMap.get(item.getId()));
            detail.setItemId(item.getId());
            detail.setImage(item.getImage());
            detail.setOrderId(orderId);
            details.add(detail);
        }
        return details;
    }
}
