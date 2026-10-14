package com.ekote.service;

import com.ekote.dto.RecordBookDTO;
import com.ekote.repositories.RecordBookRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RecordBookService {
  private final RecordBookRepository recordBookRepository;

  public RecordBookService(RecordBookRepository recordBookRepository) {
    this.recordBookRepository = recordBookRepository;
  }

  public List<RecordBookDTO> fetchRecordBook() {
    return recordBookRepository.getRecordBook();
  }
}
