package org.example.config;

import org.bson.Document;
import org.example.model.EmploymentIntentionModel;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexDefinition;
import org.springframework.data.mongodb.core.index.IndexOperations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StudentIdentityIndexConfigurationTest {
    @Test void createsAnExplicitUniqueIndexEvenWithAutoIndexCreationDisabled() throws Exception {
        MongoTemplate mongo = mock(MongoTemplate.class);
        IndexOperations indexes = mock(IndexOperations.class);
        when(mongo.indexOps(EmploymentIntentionModel.class)).thenReturn(indexes);
        new StudentIdentityIndexConfiguration().studentIdentityIndexes(mongo)
                .run(new DefaultApplicationArguments(new String[0]));
        ArgumentCaptor<IndexDefinition> definition = ArgumentCaptor.forClass(IndexDefinition.class);
        verify(indexes).ensureIndex(definition.capture());
        assertEquals(new Document("studentAccountId", 1), definition.getValue().getIndexKeys());
        Document options = definition.getValue().getIndexOptions();
        assertEquals(true, options.get("unique"));
        assertEquals("employment_student_account_unique", options.getString("name"));
        assertEquals(new Document("studentAccountId", new Document("$type", 2)), options.get("partialFilterExpression"));
    }
}
