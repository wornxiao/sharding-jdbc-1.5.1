/*
 * Copyright 1999-2015 dangdang.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package com.dangdang.ddframe.rdb.sharding.merger.orderby;

import com.dangdang.ddframe.rdb.sharding.constant.OrderType;
import com.dangdang.ddframe.rdb.sharding.merger.common.AbstractStreamResultSetMerger;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.context.OrderItem;
import lombok.AccessLevel;
import lombok.Getter;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 * 排序归并结果集接口.
 *
 * @author zhangliang
 */
@Getter(AccessLevel.PROTECTED)
public class OrderByStreamResultSetMerger extends AbstractStreamResultSetMerger {
    
    @Getter(AccessLevel.NONE)
    private final List<OrderItem> orderByItems;

    private final Queue<OrderByValue> orderByValuesQueue;
    
    private final OrderType nullOrderType;
    
    private boolean isFirstNext;
    
    public OrderByStreamResultSetMerger(final List<ResultSet> resultSets, final List<OrderItem> orderByItems, final OrderType nullOrderType) throws SQLException {
        this.orderByItems = orderByItems;
        //    排序这里是用优先级队列实现
        this.orderByValuesQueue = new PriorityQueue<>(resultSets.size());
        this.nullOrderType = nullOrderType;
//        把要排序的结果集往队列里放-》
        orderResultSetsToQueue(resultSets);
        isFirstNext = true;
    }
    
    private void orderResultSetsToQueue(final List<ResultSet> resultSets) throws SQLException {
        for (ResultSet each : resultSets) {
            OrderByValue orderByValue = new OrderByValue(each, orderByItems, nullOrderType);
            if (orderByValue.next()) {
                orderByValuesQueue.offer(orderByValue);
            }
        }
//        流式结果集归并，设置当前的流式归并结果集，大家看这里存储是当前的结果集所以不会出现内存溢出问题
        setCurrentResultSet(orderByValuesQueue.isEmpty() ? resultSets.get(0) : orderByValuesQueue.peek().getResultSet());
    }
    
    @Override
    public boolean next() throws SQLException {
        if (orderByValuesQueue.isEmpty()) {
            return false;
        }
        if (isFirstNext) {
            isFirstNext = false;
            return true;
        }
        //这里实现的排序
        OrderByValue firstOrderByValue = orderByValuesQueue.poll();//移除上一次获取的reultSet
        if (firstOrderByValue.next()) {
            orderByValuesQueue.offer(firstOrderByValue);//把排序值添加到队列
        }
        if (orderByValuesQueue.isEmpty()) {
            return false;
        }
        setCurrentResultSet(orderByValuesQueue.peek().getResultSet());
        return true;
    }
}
