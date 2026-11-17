package com.apartment.tracker.complaint;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ComplaintCodeService {
    private static final Pattern CODE_NUMBER = Pattern.compile("^[A-Z](\\d+)$");

    private final ComplaintCodeSequenceRepository sequenceRepository;
    private final ComplaintRepository complaintRepository;

    public ComplaintCodeService(
        ComplaintCodeSequenceRepository sequenceRepository,
        ComplaintRepository complaintRepository) {
        this.sequenceRepository = sequenceRepository;
        this.complaintRepository = complaintRepository;
    }

    @Transactional
    public String nextCode(ComplaintCategory category) {
        ComplaintCodeSequence sequence = sequenceRepository.findByCategoryForUpdate(category.name())
            .orElseThrow(() -> new IllegalStateException("Complaint code sequence is not initialized."));
        int number = sequence.getNextNumber();
        sequence.setNextNumber(number + 1);
        return category.getCodePrefix() + number;
    }

    @Transactional
    public void backfillExistingCodes() {
        for (ComplaintCategory category : ComplaintCategory.values()) {
            ComplaintCodeSequence sequence = sequenceRepository.findByCategoryForUpdate(category.name())
                .orElseGet(() -> sequenceRepository.save(new ComplaintCodeSequence(category.name(), 1)));
            int nextNumber = sequence.getNextNumber();
            for (Complaint complaint : complaintRepository.findByCategoryOrderByIdAsc(category)) {
                if (complaint.getComplaintCode() == null) {
                    complaint.setComplaintCode(category.getCodePrefix() + nextNumber++);
                } else {
                    Matcher matcher = CODE_NUMBER.matcher(complaint.getComplaintCode());
                    if (matcher.matches()) {
                        nextNumber = Math.max(nextNumber, Integer.parseInt(matcher.group(1)) + 1);
                    }
                }
            }
            sequence.setNextNumber(nextNumber);
        }
    }
}
