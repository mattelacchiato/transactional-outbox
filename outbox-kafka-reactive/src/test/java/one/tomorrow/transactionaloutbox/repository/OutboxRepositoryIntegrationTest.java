package one.tomorrow.transactionaloutbox.repository;

import one.tomorrow.transactionaloutbox.AbstractIntegrationTest;
import one.tomorrow.transactionaloutbox.model.OutboxRecord;
import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static one.tomorrow.transactionaloutbox.TestUtils.newRecord;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.beans.SamePropertyValuesAs.samePropertyValuesAs;

@FlywayTest
@SuppressWarnings("ConstantConditions")
class OutboxRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private OutboxRepository testee;

    @Test
    void should_UpdateRecord() {
        // given
        OutboxRecord record = testee.save(
                newRecord("topic1", "key1", "value1", Map.of("h1", "v1"))
        ).block();
        assertThat(record.getProcessed(), is(nullValue()));

        // when
        OutboxRecord update = record.toBuilder().processed(Instant.now()).build();
        testee.save(update).block();

        // then
        OutboxRecord foundUpdate = testee.findById(record.getId()).block();
        assertThat(foundUpdate.getProcessed(), is(notNullValue()));
    }

    @Test
    void should_FindUnprocessedRecords() {
        // given
        testee.save(
                newRecord(Instant.now(),"topic1", "key1", "value1", Map.of("h1", "v1"))
        ).block();

        OutboxRecord record2 = testee.save(
                newRecord("topic2", "key2", "value2", Map.of("h2", "v2"))
        ).block();

        // when
        List<OutboxRecord> result = testee.getUnprocessedRecords(100).collectList().block();

        // then
        assertThat(result.size(), is(1));
        OutboxRecord foundRecord = result.get(0);
        assertThat(foundRecord, samePropertyValuesAs(record2, "headers")); // ignore headers, because Json doesn't implement equals
        assertThat(foundRecord.getHeadersAsMap(), is(equalTo(record2.getHeadersAsMap())));
    }

}
