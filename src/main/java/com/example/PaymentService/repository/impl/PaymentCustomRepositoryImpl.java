package com.example.PaymentService.repository.impl;

import com.example.PaymentService.entity.Payment;
import com.example.PaymentService.enums.Status;
import com.example.PaymentService.repository.PaymentCustomRepository;
import lombok.RequiredArgsConstructor;
import org.bson.types.Decimal128;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.bson.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RequiredArgsConstructor
public class PaymentCustomRepositoryImpl implements PaymentCustomRepository {

    private final MongoTemplate mongoTemplate;

    @Override
    public List<Payment> search(Long userId, Long orderId, Status status) {

        Query query = new Query();

        if (userId != null) {
            query.addCriteria(Criteria.where("user_id").is(userId));
        }

        if (orderId != null) {
            query.addCriteria(Criteria.where("order_id").is(orderId));
        }

        if (status != null) {
            query.addCriteria(Criteria.where("status").is(status));
        }

        return mongoTemplate.find(query, Payment.class);
    }

    @Override
    public BigDecimal getTotalPaymentsByUserId(Long userId, Instant from, Instant to) {

        MatchOperation match = Aggregation.match(
                Criteria.where("user_id").is(userId)
                        .and("createdAt").gte(from).lte(to)
        );

        GroupOperation group = Aggregation.group().sum("payment_amount").as("total");

        Aggregation aggregation = Aggregation.newAggregation(match, group);

        AggregationResults<Document> result =
                mongoTemplate.aggregate(aggregation, "payments", Document.class);

        Document doc = result.getUniqueMappedResult();

        if(doc == null){
            return BigDecimal.ZERO;
        }

        Decimal128 decimal = doc.get("total", Decimal128.class);
        return decimal.bigDecimalValue();
    }

    @Override
    public BigDecimal getTotalPayments(Instant from, Instant to) {

        MatchOperation match = Aggregation.match(
                Criteria.where("createdAt").gte(from).lte(to)
        );

        GroupOperation group = Aggregation.group().sum("payment_amount").as("total");

        Aggregation aggregation = Aggregation.newAggregation(match, group);

        AggregationResults<Document> result =
                mongoTemplate.aggregate(aggregation, "payments", Document.class);

        Document doc = result.getUniqueMappedResult();

        if(doc == null){
            return BigDecimal.ZERO;
        }

        Decimal128 decimal = doc.get("total", Decimal128.class);
        return decimal.bigDecimalValue();
    }
}
