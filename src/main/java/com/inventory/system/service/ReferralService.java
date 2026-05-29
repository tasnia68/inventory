package com.inventory.system.service;

import com.inventory.system.payload.ApplyReferralRequest;
import com.inventory.system.payload.ReferralAttributionDto;
import com.inventory.system.payload.ReferralCodeDto;
import com.inventory.system.payload.ReferralProgramDto;
import com.inventory.system.payload.UpsertReferralProgramRequest;

import java.util.List;
import java.util.UUID;

public interface ReferralService {

    ReferralProgramDto upsertProgram(UpsertReferralProgramRequest request);

    ReferralProgramDto getProgram();

    ReferralCodeDto getOrCreateCodeForCustomer(UUID customerId);

    List<ReferralCodeDto> getCodesForCustomer(UUID customerId);

    ReferralAttributionDto attribute(ApplyReferralRequest request);

    List<ReferralAttributionDto> listAttributionsForCode(UUID referralCodeId);
}
