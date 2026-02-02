# Requirements Document

## Introduction

The Agama-Inji-Wallet integration enables an OpenID Provider to accept credential presentations from the MOSIP Inji web wallet using the OpenID for Verifiable Presentations (OpenID4VP) protocol. This system allows Janssen's Agama orchestration engine to coordinate verification workflows between users, the Inji Web wallet, and the Inji Verify service for validating verifiable credentials in real-world digital identity ecosystems.

## Glossary

- **Agama_Flow**: The Janssen orchestration engine that coordinates the entire verification workflow
- **Inji_Web**: The user's credential wallet application responsible for holding and presenting verifiable credentials
- **Inji_Verify**: The verification service responsible for validating presented verifiable credentials
- **OpenID4VP**: OpenID for Verifiable Presentations protocol used for credential verification
- **VC**: Verifiable Credential - a tamper-evident credential with authorship that can be cryptographically verified
- **VP**: Verifiable Presentation - a tamper-evident presentation derived from one or more verifiable credentials
- **RFAC**: Jans Authorization Challenge endpoint for secure authorization handling
- **VC_Verifier_Library**: The library used by Inji Verify to validate verifiable presentations

## Requirements

### Requirement 1

**User Story:** As a user, I want to initiate credential verification through Agama so that I can prove my identity using credentials stored in my Inji wallet.

#### Acceptance Criteria

1. WHEN a user clicks "Verify with Inji", THE Agama_Flow SHALL create an authorization request to Inji_Verify
2. THE Agama_Flow SHALL send a POST request to the endpoint "/verifyServiceURL/vp-request"
3. WHEN the authorization response is received, THE Agama_Flow SHALL store the requestId and transactionId values in the flow context
4. THE Agama_Flow SHALL construct an OpenID4VP request containing nonce, state, presentation_definition, and response_url parameters
5. THE Agama_Flow SHALL redirect the user to Inji_Web with the constructed OpenID4VP request

### Requirement 2

**User Story:** As a user, I want to authenticate and select credentials in Inji Web so that I can choose which credentials to present for verification.

#### Acceptance Criteria

1. WHEN a user is redirected to Inji_Web and is not authenticated, THE Inji_Web SHALL prompt for user authentication
2. WHEN a user is authenticated, THE Inji_Web SHALL display available credentials for selection
3. WHEN a user selects credentials and provides consent, THE Inji_Web SHALL send a VP token response to Inji_Verify
4. WHEN the VP token is sent, THE Inji_Web SHALL redirect the user back to Agama_Flow

### Requirement 3

**User Story:** As an Agama flow, I want to monitor the verification status so that I can determine when a verifiable presentation has been submitted and processed.

#### Acceptance Criteria

1. THE Agama_Flow SHALL perform a GET request to "/vp-request/{requestId}/status" to check verification status
2. WHEN the status is "ACTIVE", THE Agama_Flow SHALL wait and retry after a configurable delay
3. WHEN the status is "VP_SUBMITTED", THE Agama_Flow SHALL proceed to fetch the verification result
4. WHEN the status is "EXPIRED", THE Agama_Flow SHALL terminate the flow with an appropriate error message

### Requirement 4

**User Story:** As an Agama flow, I want to retrieve and validate verification results so that I can determine if the presented credentials are valid.

#### Acceptance Criteria

1. WHEN the VP status is "VP_SUBMITTED", THE Agama_Flow SHALL call "/vp-result/{transactionId}" to fetch the result
2. THE Inji_Verify SHALL validate the VP using the VC_Verifier_Library and return either "valid" or "invalid"
3. THE Agama_Flow SHALL parse the validation response and update the flow state accordingly
4. THE Agama_Flow SHALL log the validation outcome for audit purposes

### Requirement 5

**User Story:** As a user, I want to receive clear feedback about my verification attempt so that I understand whether my credential verification succeeded or failed.

#### Acceptance Criteria

1. WHEN the VC validation result is "valid", THE Agama_Flow SHALL display "Verification successful" message
2. WHEN the VC validation result is "invalid", THE Agama_Flow SHALL display "Verification failed" message with appropriate error reason
3. THE Agama_Flow SHALL ensure all user messages are user-friendly and localized
4. THE Agama_Flow SHALL provide clear next steps or instructions based on the verification outcome

### Requirement 6

**User Story:** As a system administrator, I want comprehensive error handling and logging so that I can diagnose and resolve issues in the verification workflow.

#### Acceptance Criteria

1. WHEN any REST API call fails with timeout, 4xx, or 5xx errors, THE Agama_Flow SHALL log the failure with timestamp and endpoint details
2. THE Agama_Flow SHALL gracefully handle failures and display meaningful error messages to users
3. THE Agama_Flow SHALL implement configurable retry mechanisms for transient failures during status polling
4. THE Agama_Flow SHALL maintain audit logs for all verification attempts and outcomes

### Requirement 7

**User Story:** As an Agama flow, I want to integrate with the Jans Authorization Challenge (RFAC) endpoint so that I can handle authorization securely according to Jans Server specifications.

#### Acceptance Criteria

1. THE Agama_Flow SHALL handle RFAC request and response according to Jans Server specification
2. THE Agama_Flow SHALL correlate RFAC interactions with requestId and transactionId in the flow context
3. THE Agama_Flow SHALL log challenge results for traceability and audit purposes
4. THE Agama_Flow SHALL ensure secure handling of authorization challenges throughout the verification process