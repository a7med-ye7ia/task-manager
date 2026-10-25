# Phase 1: Accounts and Sign-in Implementation Status

This document tracks the implementation status of Phase 1 requirements from `docs/SRS.md` Section 11, referencing business rules (BR-6 to BR-13, auth parts of BR-40 and BR-41) and frontend screens from Section 5.

Status definitions:
- **Done**: Fully implemented, validated with tests, adheres to SRS.
- **Partial**: Backend or frontend implemented, pending full integration, styling, or unit tests.
- **Missing**: Not yet started or implemented.

---

## 1. Business Rules

| Rule | Description | Status | Implementation Files |
| :--- | :--- | :--- | :--- |
| **BR-6** | Manager adds an employee (Invite, 48h token, Set Password flow, Resend invite) | **Done** | `backend/.../user/service/UserAdminService.java`<br>`backend/.../auth/service/AuthService.java`<br>`backend/.../email/service/EmailSenderJob.java`<br>`backend/.../user/UserAdminIntegrationTest.java` |
| **BR-7** | Self sign-up (PENDING_EMAIL, 24h token, PENDING_APPROVAL, Manager approval/rejection with optional reason, notifications) | **Done** | `backend/.../auth/service/AuthService.java`<br>`backend/.../user/service/UserAdminService.java`<br>`backend/.../email/service/EmailSenderJob.java`<br>`backend/.../auth/AuthIntegrationTest.java`<br>`backend/.../user/UserAdminIntegrationTest.java` |
| **BR-8** | Login & sessions (Email + password, 5-attempt/15-min lockout, 15m JWT, 7d rotated httpOnly refresh token cookie, clear status messages for non-ACTIVE) | **Done** | `backend/.../auth/service/AuthService.java`<br>`backend/.../auth/service/JwtService.java`<br>`backend/.../auth/service/JwtAuthenticationFilter.java`<br>`backend/.../auth/AuthIntegrationTest.java` |
| **BR-9** | Forgot password (Generic response, 32-byte hash token, 30m expiry, 5 req/hr limit, Reset password requires different password, revokes sessions, confirmation email) | **Done** | `backend/.../auth/service/AuthService.java`<br>`backend/.../auth/service/RateLimiterService.java`<br>`backend/.../auth/AuthIntegrationTest.java` |
| **BR-10**| Change password from profile (Current password required, new password must differ, revokes other sessions) | **Done** | `backend/.../user/service/UserService.java`<br>`backend/.../user/UserProfileIntegrationTest.java` |
| **BR-11**| Profile and email change (Update names, phone, email confirmation link to new email with 24h token while old email remains active) | **Done** | `backend/.../user/service/UserService.java`<br>`backend/.../auth/service/AuthService.java`<br>`backend/.../user/UserProfileIntegrationTest.java` |
| **BR-12**| Avatar (JPG/PNG/WebP magic byte + ImageIO verification, max 2MB, stored in protected upload dir, old avatar replaced/deleted) | **Done** | `backend/.../user/service/AvatarService.java`<br>`backend/.../user/service/UserService.java`<br>`backend/.../user/UserProfileIntegrationTest.java` |
| **BR-13**| Deactivate, delete, restore (DISABLED status, soft-delete, restore to ACTIVE, cleans task_members and subtask_assignees, audit_log records) | **Done** | `backend/.../user/service/UserAdminService.java`<br>`backend/.../audit/service/AuditLogService.java`<br>`backend/.../user/UserAdminIntegrationTest.java` |
| **BR-40**| Format & validation (Arabic/English names 1-60 chars, Gmail addresses only, Egyptian phone format +20 / 01..., password 8-64 chars with uppercase, lowercase, digit, non-blank) | **Done** | `backend/.../common/validation/ValidationUtils.java`<br>`backend/.../auth/AuthIntegrationTest.java` |
| **BR-41**| Rate limits (Sign-up: 10/hr per IP; Forgot password: 5/hr per email & IP) | **Done** | `backend/.../auth/service/RateLimiterService.java`<br>`backend/.../auth/AuthIntegrationTest.java` |

---

## 2. Frontend Screens (Section 5)

| Screen | Description | Status | Implementation Files |
| :--- | :--- | :--- | :--- |
| **Login** | Email & password form, status messages for non-ACTIVE, link to signup & forgot password | **Done** | `frontend/src/pages/LoginPage.tsx`<br>`frontend/src/context/AuthContext.tsx`<br>`frontend/src/services/api.ts` |
| **Sign Up** | First/last name, Gmail email, phone, password, validation errors, generic success response | **Done** | `frontend/src/pages/SignUpPage.tsx`<br>`frontend/src/services/api.ts` |
| **Email Verified** | Reads `?token=...`, validates with `/api/auth/verify-email`, shows success state / pending approval | **Done** | `frontend/src/pages/VerifyEmailPage.tsx`<br>`frontend/src/services/api.ts` |
| **Set Password (Invite)** | Accepts invite token, prompts for new password + confirmation, redirects to login | **Done** | `frontend/src/pages/SetPasswordPage.tsx`<br>`frontend/src/services/api.ts` |
| **Forgot Password** | Email input, generic confirmation message ("If the account exists, we sent a link") | **Done** | `frontend/src/pages/ForgotPasswordPage.tsx`<br>`frontend/src/services/api.ts` |
| **Reset Password** | Token validation, new password + confirmation, "Request new link" if expired | **Done** | `frontend/src/pages/ResetPasswordPage.tsx`<br>`frontend/src/services/api.ts` |
| **Profile** | Details view/edit, avatar upload/preview, change password form, Manager email preferences | **Done** | `frontend/src/pages/ProfilePage.tsx`<br>`frontend/src/services/api.ts` |
| **Users List with Filters** | Paginated table, search query, role & status filters, include soft-deleted checkbox, actions | **Done** | `frontend/src/pages/AdminUsersPage.tsx`<br>`frontend/src/services/api.ts` |
| **Add & Edit User** | Modal/form to invite new user or edit name, phone, role, email with BR-4 manager protection | **Done** | `frontend/src/components/users/UserModal.tsx`<br>`frontend/src/pages/AdminUsersPage.tsx` |
| **Pending Requests** | List of pending sign-ups, approve (role picker), reject with optional reason, badge count | **Done** | `frontend/src/pages/AdminPendingPage.tsx`<br>`frontend/src/context/AuthContext.tsx`<br>`frontend/src/services/api.ts` |
