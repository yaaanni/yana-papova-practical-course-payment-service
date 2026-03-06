package com.example.PaymentService.service.outbox;

import com.example.PaymentService.kafka.dto.OutboxRequest;
import com.example.PaymentService.kafka.entity.Outbox;
import com.example.PaymentService.kafka.enums.EventStatus;
import com.example.PaymentService.kafka.mapper.OutboxMapper;
import com.example.PaymentService.kafka.repository.OutboxRepository;
import com.example.PaymentService.kafka.service.OutboxService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class OutboxServiceTest {

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private OutboxMapper outboxMapper;

    @InjectMocks
    private OutboxService outboxService;

    @Test
    void create_shouldCreateEvent() {
        OutboxRequest outboxRequest = new OutboxRequest();
        Outbox outbox = new Outbox();

        when(outboxMapper.toEntity(outboxRequest)).thenReturn(outbox);

        outboxService.create(outboxRequest);

        verify(outboxRepository).save(outbox);
    }

    @Test
    void getNewEvents_shouldReturnListOfEvents() {
        Outbox event1 = new Outbox();
        Outbox event2 = new Outbox();

        List<Outbox> expectedEvents = List.of(event1, event2);

        when(outboxRepository.findTop100ByStatusOrderByCreatedAtAsc(EventStatus.NEW.name()))
                .thenReturn(expectedEvents);

        List<Outbox> actualEvents = outboxService.getNewEvents();

        assertEquals(expectedEvents, actualEvents);
        assertEquals(2, actualEvents.size());

        verify(outboxRepository).findTop100ByStatusOrderByCreatedAtAsc(EventStatus.NEW.name());
    }

    @Test
    void getFailedEvents_shouldReturnListOfEvents() {
        Outbox event1 = new Outbox();
        Outbox event2 = new Outbox();

        List<Outbox> expectedEvents = List.of(event1, event2);

        when(outboxRepository.findTop100ByStatusOrderByCreatedAtAsc(EventStatus.FAILED.name()))
                .thenReturn(expectedEvents);

        List<Outbox> actualEvents = outboxService.getFailedEvents();

        assertEquals(expectedEvents, actualEvents);
        assertEquals(2, actualEvents.size());

        verify(outboxRepository).findTop100ByStatusOrderByCreatedAtAsc(EventStatus.FAILED.name());
    }

    @Test
    void markAsSent_shouldOverwriteStatusToCanceled() {
        Outbox outbox = new Outbox();
        outbox.setStatus(EventStatus.NEW);

        String id = "id";

        when(outboxRepository.findById(id)).thenReturn(Optional.of(outbox));

        outboxService.markAsSent(id);

        assertEquals(EventStatus.CANCELLED, outbox.getStatus());

        verify(outboxRepository).save(outbox);
    }

    @Test
    void markAsSent_shouldThrowException_whenIdNotFound() {
        String invalidId = "not-found-id";

        when(outboxRepository.findById(invalidId)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> outboxService.markAsSent(invalidId));

        verify(outboxRepository, never()).save(any());
    }

    @Test
    void markAsFailed_shouldOverwriteStatusToFailed() {
        Outbox outbox = new Outbox();
        outbox.setStatus(EventStatus.NEW);

        String id = "id";

        when(outboxRepository.findById(id)).thenReturn(Optional.of(outbox));

        outboxService.markAsFailed(id);

        assertEquals(EventStatus.FAILED, outbox.getStatus());

        verify(outboxRepository).save(outbox);
    }

    @Test
    void markAsFailed_shouldThrowException_whenIdNotFound() {
        String invalidId = "not-found-id";

        when(outboxRepository.findById(invalidId)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> outboxService.markAsFailed(invalidId));

        verify(outboxRepository, never()).save(any());
    }
}
