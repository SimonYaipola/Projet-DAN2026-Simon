package com.craftboard.server.service;

import com.craftboard.core.dto.QuestDeliveryRequest;
import com.craftboard.core.dto.QuestDeliveryResponse;
import com.craftboard.core.dto.QuestRequest;
import com.craftboard.core.dto.QuestResponse;
import com.craftboard.core.enums.QuestRecurrence;
import com.craftboard.core.enums.QuestStatus;
import com.craftboard.core.enums.UserRole;
import com.craftboard.server.entity.AppUser;
import com.craftboard.server.entity.Quest;
import com.craftboard.server.entity.QuestSignup;
import com.craftboard.server.repository.QuestRepository;
import com.craftboard.server.repository.QuestSignupRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;

/**
 * Service metier utilise par les controleurs du serveur.
 */
@Service
@Transactional
public class QuestService {

    private final QuestRepository repository;
    private final QuestSignupRepository signupRepository;
    private final EntityLookupService lookup;
    private final ActivityLogService activityLogService;

    public QuestService(
            QuestRepository repository,
            QuestSignupRepository signupRepository,
            EntityLookupService lookup,
            ActivityLogService activityLogService) {
        this.repository = repository;
        this.signupRepository = signupRepository;
        this.lookup = lookup;
        this.activityLogService = activityLogService;
    }

    public List<QuestResponse> findAll() {
        return repository.findAll().stream()
                .map(this::resetIfDue)
                .map(this::toResponse)
                .toList();
    }

    public List<QuestResponse> findByCity(Long cityId) {
        return repository.findByCityId(cityId).stream()
                .map(this::resetIfDue)
                .map(this::toResponse)
                .toList();
    }

    public QuestResponse findById(Long id) {
        return toResponse(resetIfDue(findEntity(id)));
    }

    public QuestResponse create(QuestRequest request) {
        validateAdminCreator(request);
        Quest quest = new Quest();
        apply(quest, request);
        quest.setLastResetAt(LocalDateTime.now());
        Quest saved = repository.save(quest);
        activityLogService.record(
                saved.getCreatedBy(),
                "QUEST_CREATED",
                "Quete creee: " + saved.getTitle()
        );
        return toResponse(saved);
    }

    public QuestResponse update(Long id, QuestRequest request) {
        Quest quest = findEntity(id);
        apply(quest, request);
        return toResponse(repository.save(quest));
    }

    public QuestResponse deliver(Long questId, QuestDeliveryRequest request) {
        Quest quest = resetIfDue(findEntity(questId));
        AppUser user = lookup.user(request.userId());
        if (!quest.getCity().getId().equals(user.getCity().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User and quest must be in the same city");
        }
        int deliveredQuantity = request.deliveredQuantity() == null ? 1 : request.deliveredQuantity();
        if (deliveredQuantity <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Delivered quantity must be greater than 0");
        }
        QuestSignup signup = signupRepository.findByQuestIdAndUserId(questId, user.getId())
                .orElseGet(QuestSignup::new);
        signup.setQuest(quest);
        signup.setUser(user);
        int previousDelivery = signup.getId() == null || signup.getDeliveredQuantity() == null
                ? 0
                : signup.getDeliveredQuantity();
        signup.setDeliveredQuantity(previousDelivery + deliveredQuantity);
        signupRepository.save(signup);
        activityLogService.record(
                user,
                "QUEST_DELIVERED",
                user.getUsername() + " a livre " + deliveredQuantity + " " + quest.getResourceName() + " pour " + quest.getTitle()
        );
        Quest updatedQuest = findEntity(questId);
        completeIfTargetReached(updatedQuest, user);
        return toResponse(updatedQuest);
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Quest not found: " + id);
        }
        repository.deleteById(id);
    }

    private Quest findEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quest not found: " + id));
    }

    private void apply(Quest quest, QuestRequest request) {
        validateQuestFields(request);
        quest.setCity(lookup.city(request.cityId()));
        quest.setTitle(request.title());
        quest.setDescription(request.description());
        quest.setStatus(request.status() == null ? QuestStatus.PENDING : request.status());
        quest.setRecurrence(request.recurrence() == null ? QuestRecurrence.ONCE : request.recurrence());
        quest.setResourceName(request.resourceName());
        quest.setTargetQuantity(request.targetQuantity() == null ? 1 : request.targetQuantity());
        quest.setCreatedBy(request.createdBy() == null ? null : lookup.user(request.createdBy()));
        if (quest.getLastResetAt() == null) {
            quest.setLastResetAt(LocalDateTime.now());
        }
    }

    private Quest resetIfDue(Quest quest) {
        if (quest.getRecurrence() == QuestRecurrence.ONCE) {
            return quest;
        }
        LocalDateTime lastResetAt = quest.getLastResetAt() == null
                ? (quest.getCreatedAt() == null ? LocalDateTime.now() : quest.getCreatedAt())
                : quest.getLastResetAt();
        if (!isResetDue(quest.getRecurrence(), lastResetAt, LocalDateTime.now())) {
            return quest;
        }
        quest.getSignups().clear();
        quest.setStatus(QuestStatus.PENDING);
        quest.setLastResetAt(LocalDateTime.now());
        Quest saved = repository.save(quest);
        if (saved.getCreatedBy() != null) {
            activityLogService.record(saved.getCreatedBy(), "QUEST_RESET", "Quete reinitialisee: " + saved.getTitle());
        }
        return saved;
    }

    private boolean isResetDue(QuestRecurrence recurrence, LocalDateTime lastResetAt, LocalDateTime now) {
        if (recurrence == QuestRecurrence.DAILY) {
            return lastResetAt.toLocalDate().isBefore(now.toLocalDate());
        }
        if (recurrence == QuestRecurrence.WEEKLY) {
            WeekFields weekFields = WeekFields.of(Locale.CANADA_FRENCH);
            LocalDate last = lastResetAt.toLocalDate();
            LocalDate current = now.toLocalDate();
            int lastWeek = last.get(weekFields.weekOfWeekBasedYear());
            int currentWeek = current.get(weekFields.weekOfWeekBasedYear());
            int lastYear = last.get(weekFields.weekBasedYear());
            int currentYear = current.get(weekFields.weekBasedYear());
            return lastYear != currentYear || lastWeek != currentWeek;
        }
        return false;
    }

    private void completeIfTargetReached(Quest quest, AppUser user) {
        int deliveredQuantity = quest.getSignups().stream()
                .mapToInt(signup -> signup.getDeliveredQuantity() == null ? 0 : signup.getDeliveredQuantity())
                .sum();
        int targetQuantity = quest.getTargetQuantity() == null ? 0 : quest.getTargetQuantity();
        if (targetQuantity > 0 && deliveredQuantity >= targetQuantity && quest.getStatus() != QuestStatus.COMPLETED) {
            quest.setStatus(QuestStatus.COMPLETED);
            repository.save(quest);
            activityLogService.record(user, "QUEST_COMPLETED", "Quete completee: " + quest.getTitle());
        }
    }

    private void validateAdminCreator(QuestRequest request) {
        if (request.createdBy() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "createdBy is required");
        }
        AppUser creator = lookup.user(request.createdBy());
        if (creator.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins can create quests");
        }
        if (!creator.getCity().getId().equals(request.cityId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Creator and quest must be in the same city");
        }
    }

    private void validateQuestFields(QuestRequest request) {
        if (request.cityId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cityId is required");
        }
        if (request.title() == null || request.title().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title is required");
        }
        if (request.resourceName() == null || request.resourceName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Resource is required");
        }
        if (request.targetQuantity() != null && request.targetQuantity() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target quantity must be greater than 0");
        }
    }

    private QuestResponse toResponse(Quest quest) {
        List<QuestDeliveryResponse> deliveries = quest.getSignups().stream()
                .map(this::toDeliveryResponse)
                .toList();
        int deliveredQuantity = deliveries.stream()
                .mapToInt(delivery -> delivery.deliveredQuantity() == null ? 0 : delivery.deliveredQuantity())
                .sum();
        int targetQuantity = quest.getTargetQuantity() == null ? 0 : quest.getTargetQuantity();
        return new QuestResponse(
                quest.getId(),
                quest.getCity().getId(),
                quest.getTitle(),
                quest.getDescription(),
                quest.getStatus(),
                quest.getRecurrence(),
                quest.getResourceName(),
                quest.getTargetQuantity(),
                deliveredQuantity,
                Math.max(0, targetQuantity - deliveredQuantity),
                quest.getCreatedBy() == null ? null : quest.getCreatedBy().getId(),
                quest.getCreatedAt(),
                deliveries
        );
    }

    private QuestDeliveryResponse toDeliveryResponse(QuestSignup signup) {
        return new QuestDeliveryResponse(
                signup.getId(),
                signup.getQuest().getId(),
                signup.getUser().getId(),
                signup.getUser().getUsername(),
                signup.getDeliveredQuantity(),
                signup.getCreatedAt()
        );
    }
}
