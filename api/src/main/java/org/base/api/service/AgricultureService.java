package org.base.api.service;

import lombok.RequiredArgsConstructor;
import org.base.api.repository.mysql.AgricultureDataRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor // This creates a constructor for our final fields
public class AgricultureService {

    // Spring will automatically inject the repository we created
    private final AgricultureDataRepository agricultureDataRepository;

    // We will add methods here to handle the business logic
    // for the "Soplis meurneoba" feature.
    // For example, a method to process the uploaded file will go here.
}