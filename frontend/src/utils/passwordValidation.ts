export interface PasswordValidationResult {
  isValid: boolean;
  hasLength: boolean;
  hasUppercase: boolean;
  hasLowercase: boolean;
  hasDigit: boolean;
  hasSpecial: boolean;
  errorMessage?: string;
}

export const validatePassword = (password: string, email?: string): PasswordValidationResult => {
  const hasLength = password.length >= 8 && password.length <= 64;
  const hasUppercase = /[A-Z]/.test(password);
  const hasLowercase = /[a-z]/.test(password);
  const hasDigit = /[0-9]/.test(password);
  const hasSpecial = /[^a-zA-Z0-9]/.test(password);

  let errorMessage: string | undefined;

  if (!password) {
    errorMessage = 'Password is required';
  } else if (password.startsWith(' ') || password.endsWith(' ')) {
    errorMessage = 'Password must not contain leading or trailing spaces';
  } else if (!hasLength) {
    errorMessage = 'Password must be between 8 and 64 characters';
  } else if (!hasUppercase || !hasLowercase || !hasDigit || !hasSpecial) {
    errorMessage = 'Password must include uppercase, lowercase, a number, and at least one special character';
  } else if (email && password.toLowerCase() === email.trim().toLowerCase()) {
    errorMessage = 'Password cannot be equal to your email address';
  }

  return {
    isValid: !errorMessage,
    hasLength,
    hasUppercase,
    hasLowercase,
    hasDigit,
    hasSpecial,
    errorMessage,
  };
};
