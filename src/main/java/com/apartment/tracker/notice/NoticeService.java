package com.apartment.tracker.notice;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class NoticeService {
    private final NoticeRepository repository;

    public NoticeService(NoticeRepository repository) {
        this.repository = repository;
    }

    public List<Notice> getAll() {
        return repository.findAllByOrderByPublishedAtDesc();
    }

    public List<Notice> getLatest() {
        return repository.findTop3ByOrderByPublishedAtDesc();
    }

    public void publish(String title, String description, String publishedBy) {
        Notice notice = new Notice();
        notice.setTitle(title.trim());
        notice.setDescription(description.trim());
        notice.setPublishedBy(publishedBy);
        repository.save(notice);
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("Notice not found: " + id);
        }
        repository.deleteById(id);
    }
}
