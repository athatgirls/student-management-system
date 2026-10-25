package org.example.config;

import org.example.model.EmploymentIntentionModel;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.PartialIndexFilter;
import org.springframework.data.mongodb.core.query.Criteria;

/** Do not rely on MongoDB's disabled-by-default annotation index creation. */
@Configuration
public class StudentIdentityIndexConfiguration {
    public static final String INTENTION_INDEX = "employment_student_account_unique";

    @Bean
    ApplicationRunner studentIdentityIndexes(MongoTemplate mongo) {
        return args -> mongo.indexOps(EmploymentIntentionModel.class).ensureIndex(
                new Index().on("studentAccountId", Sort.Direction.ASC).unique()
                        .named(INTENTION_INDEX)
                        .partial(PartialIndexFilter.of(Criteria.where("studentAccountId").type(2))));
    }
}
