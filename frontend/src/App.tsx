import React from 'react';
import { BrowserRouter, Routes, Route, Navigate, useLocation } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from './context/AuthContext';
import { AppLayout } from './components/layout/AppLayout';
import { TaskPoolPage } from './pages/TaskPoolPage';
import { TaskDetailPage } from './pages/TaskDetailPage';
import { LoginPage } from './pages/LoginPage';
import { SignUpPage } from './pages/SignUpPage';
import { VerifyEmailPage } from './pages/VerifyEmailPage';
import { SetPasswordPage } from './pages/SetPasswordPage';
import { ForgotPasswordPage } from './pages/ForgotPasswordPage';
import { ResetPasswordPage } from './pages/ResetPasswordPage';
import { ProfilePage } from './pages/ProfilePage';
import { AdminUsersPage } from './pages/AdminUsersPage';
import { AdminPendingPage } from './pages/AdminPendingPage';
import { UserDetailsPage } from './pages/UserDetailsPage';
import { NotFoundPage } from './pages/NotFoundPage';
import { useAuth } from './context/AuthContext';
import { AppLoader } from './components/common/LoadingStates';

const Protected: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { isAuthenticated, isLoading } = useAuth(); const location = useLocation();
  if (isLoading) return <AppLoader />;
  return isAuthenticated ? <>{children}</> : <Navigate to="/login" replace state={{ from: location, notice: 'Sign in to continue.' }} />;
};

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
    },
  },
});

export const App: React.FC = () => {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <BrowserRouter>
          <Routes>
            <Route path="/" element={<AppLayout />}>
              <Route index element={<Protected><TaskPoolPage /></Protected>} />
              <Route path="tasks" element={<Protected><TaskPoolPage /></Protected>} />
              <Route path="tasks/:id" element={<Protected><TaskDetailPage /></Protected>} />
              <Route path="login" element={<LoginPage />} />
              <Route path="signup" element={<SignUpPage />} />
              <Route path="verify-email" element={<VerifyEmailPage />} />
              <Route path="set-password" element={<SetPasswordPage />} />
              <Route path="forgot-password" element={<ForgotPasswordPage />} />
              <Route path="reset-password" element={<ResetPasswordPage />} />
              <Route path="profile" element={<Protected><ProfilePage /></Protected>} />
              <Route path="admin/users" element={<Protected><AdminUsersPage /></Protected>} />
              <Route path="admin/users/:id" element={<Protected><UserDetailsPage /></Protected>} />
              <Route path="users/:id" element={<Protected><UserDetailsPage /></Protected>} />
              <Route path="admin/pending" element={<Protected><AdminPendingPage /></Protected>} />
              <Route path="*" element={<NotFoundPage />} />
            </Route>
          </Routes>
        </BrowserRouter>
      </AuthProvider>
    </QueryClientProvider>
  );
};

export default App;
