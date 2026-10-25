import React, { useState, useEffect } from 'react';
import { User, Role } from '../../types';
import { inviteUserApi, editUserApi, ApiError } from '../../services/api';
import { Modal } from '../common/Modal';
import { Button } from '../common/Button';
import { Input } from '../common/Input';
import { User as UserIcon, Mail, Phone, Calendar, AlertCircle } from 'lucide-react';
import { Select } from '../common/Select';

interface UserModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSaved: (message: string) => void;
  userToEdit?: User | null;
}

export const UserModal: React.FC<UserModalProps> = ({
  isOpen,
  onClose,
  onSaved,
  userToEdit,
}) => {
  const isEditing = !!userToEdit;

  const [formData, setFormData] = useState({
    firstName: '',
    lastName: '',
    email: '',
    phone: '',
    role: 'EMPLOYEE' as Role,
    hiredAt: '',
  });

  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [generalError, setGeneralError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (userToEdit) {
      setFormData({
        firstName: userToEdit.firstName || '',
        lastName: userToEdit.lastName || '',
        email: userToEdit.email || '',
        phone: userToEdit.phone || '',
        role: userToEdit.role || 'EMPLOYEE',
        hiredAt: userToEdit.hiredAt || '',
      });
    } else {
      setFormData({
        firstName: '',
        lastName: '',
        email: '',
        phone: '',
        role: 'EMPLOYEE',
        hiredAt: new Date().toISOString().split('T')[0],
      });
    }
    setFieldErrors({});
    setGeneralError(null);
  }, [userToEdit, isOpen]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (fieldErrors[name]) {
      setFieldErrors((prev) => {
        const next = { ...prev };
        delete next[name];
        return next;
      });
    }
  };

  const validate = (): boolean => {
    const errors: Record<string, string> = {};

    if (!formData.firstName.trim()) {
      errors.firstName = 'First name is required';
    }
    if (!formData.lastName.trim()) {
      errors.lastName = 'Last name is required';
    }
    if (!formData.email.trim()) {
      errors.email = 'Email is required';
    } else if (!formData.email.toLowerCase().endsWith('@gmail.com')) {
      errors.email = 'Only Gmail addresses (@gmail.com) are accepted';
    }

    if (!formData.phone.trim()) {
      errors.phone = 'Phone number is required';
    }

    if (!isEditing && !formData.hiredAt) {
      errors.hiredAt = 'Hired date is required';
    }

    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setGeneralError(null);

    if (!validate()) return;

    setIsSubmitting(true);

    try {
      if (isEditing && userToEdit) {
        await editUserApi(userToEdit.id, {
          firstName: formData.firstName.trim(),
          lastName: formData.lastName.trim(),
          email: formData.email.trim().toLowerCase(),
          phone: formData.phone.trim(),
          role: formData.role,
        });
        onSaved('User updated successfully.');
      } else {
        await inviteUserApi({
          firstName: formData.firstName.trim(),
          lastName: formData.lastName.trim(),
          email: formData.email.trim().toLowerCase(),
          phone: formData.phone.trim(),
          role: formData.role,
          hiredAt: formData.hiredAt,
        });
        onSaved('Invitation sent successfully.');
      }
      onClose();
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.fieldErrors && err.fieldErrors.length > 0) {
          const map: Record<string, string> = {};
          err.fieldErrors.forEach((f) => {
            map[f.field] = f.message;
          });
          setFieldErrors(map);
        } else {
          setGeneralError(err.message);
        }
      } else if (err instanceof Error) {
        setGeneralError(err.message);
      } else {
        setGeneralError('Failed to save user. Please try again.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={isEditing ? `Edit User (${userToEdit?.email})` : 'Invite New Employee'}
      maxWidth="500px"
    >
      {generalError && (
        <div
          style={{
            padding: '0.85rem 1rem',
            borderRadius: 'var(--radius-button)',
            backgroundColor: 'var(--danger-bg)',
            border: '1px solid #fecaca',
            color: 'var(--danger)',
            fontSize: '0.85rem',
            marginBottom: '1.25rem',
            display: 'flex',
            alignItems: 'center',
            gap: '0.5rem',
          }}
        >
          <AlertCircle size={18} style={{ flexShrink: 0 }} />
          <span>{generalError}</span>
        </div>
      )}

      <form noValidate onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
        <div className="responsive-form-grid" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
          <Input
            label="First Name"
            name="firstName"
            required
            value={formData.firstName}
            onChange={handleChange}
            placeholder="e.g. Omar"
            icon={<UserIcon size={18} />}
            error={fieldErrors.firstName}
          />
          <Input
            label="Last Name"
            name="lastName"
            required
            value={formData.lastName}
            onChange={handleChange}
            placeholder="e.g. Hassan"
            icon={<UserIcon size={18} />}
            error={fieldErrors.lastName}
          />
        </div>

        <Input
          label="Gmail Address"
          type="email"
          name="email"
          required
          value={formData.email}
          onChange={handleChange}
          placeholder="username@gmail.com"
          icon={<Mail size={18} />}
          error={fieldErrors.email}
          helperText={isEditing ? 'Changing email will send a confirmation link to the new address.' : undefined}
        />

        <Input
          label="Egyptian Mobile Number"
          type="tel"
          name="phone"
          required
          value={formData.phone}
          onChange={handleChange}
          placeholder="01012345678 or +201012345678"
          icon={<Phone size={18} />}
          error={fieldErrors.phone}
        />

        <div className="responsive-form-grid" style={{ display: 'grid', gridTemplateColumns: isEditing ? '1fr' : '1fr 1fr', gap: '1rem' }}>
          <div>
            <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.4rem', color: 'var(--text-main)' }}>
              Role
            </label>
            <Select value={formData.role} onValueChange={(role) => setFormData((prev) => ({ ...prev, role: role as Role }))} options={[{ value: 'EMPLOYEE', label: 'Employee' }, { value: 'MANAGER', label: 'Manager' }]} ariaLabel="Role" />
          </div>

          {!isEditing && (
            <Input
              label="Hired Date"
              type="date"
              name="hiredAt"
              required
              value={formData.hiredAt}
              onChange={handleChange}
              icon={<Calendar size={18} />}
              error={fieldErrors.hiredAt}
            />
          )}
        </div>

        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '1rem' }}>
          <Button
            type="button"
            variant="outline"
            onClick={onClose}
            disabled={isSubmitting}
          >
            Cancel
          </Button>
          <Button
            type="submit"
            variant="primary"
            isLoading={isSubmitting}
          >
            {isEditing ? 'Save Changes' : 'Send Invitation'}
          </Button>
        </div>
      </form>
    </Modal>
  );
};
