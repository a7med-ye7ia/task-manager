import React, { Suspense, lazy } from 'react';
import { BrowserRouter, Routes, Route, Navigate, useLocation } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider, useAuth } from './context/AuthContext';
import { AppLayout } from './components/layout/AppLayout';
import { AppLoader, PageLoader } from './components/common/LoadingStates';

// Route-level code splitting with React.lazy
const TaskPoolPage = lazy(() => import('./pages/TaskPoolPage').then((m) => ({ default: m.TaskPoolPage })));
const TaskDetailPage = lazy(() => import('./pages/TaskDetailPage').then((m) => ({ default: m.TaskDetailPage })));
const TaskFilePage = lazy(() => import('./pages/TaskFilePage').then((m) => ({ default: m.TaskFilePage })));
const LoginPage = lazy(() => import('./pages/LoginPage').then((m) => ({ default: m.LoginPage })));
const SignUpPage = lazy(() => import('./pages/SignUpPage').then((m) => ({ default: m.SignUpPage })));
const VerifyEmailPage = lazy(() => import('./pages/VerifyEmailPage').then((m) => ({ default: m.VerifyEmailPage })));
const SetPasswordPage = lazy(() => import('./pages/SetPasswordPage').then((m) => ({ default: m.SetPasswordPage })));
const ForgotPasswordPage = lazy(() => import('./pages/ForgotPasswordPage').then((m) => ({ default: m.ForgotPasswordPage })));
const ResetPasswordPage = lazy(() => import('./pages/ResetPasswordPage').then((m) => ({ default: m.ResetPasswordPage })));
const ProfilePage = lazy(() => import('./pages/ProfilePage').then((m) => ({ default: m.ProfilePage })));
const MyTimePage = lazy(() => import('./pages/MyTimePage').then((m) => ({ default: m.MyTimePage })));
const AdminUsersPage = lazy(() => import('./pages/AdminUsersPage').then((m) => ({ default: m.AdminUsersPage })));
const AdminPendingPage = lazy(() => import('./pages/AdminPendingPage').then((m) => ({ default: m.AdminPendingPage })));
const UserDetailsPage = lazy(() => import('./pages/UserDetailsPage').then((m) => ({ default: m.UserDetailsPage })));
const NotFoundPage = lazy(() => import('./pages/NotFoundPage').then((m) => ({ default: m.NotFoundPage })));

const Protected: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { isAuthenticated, isLoading } = useAuth();
  const location = useLocation();
  if (isLoading) return <AppLoader />;
  return isAuthenticated ? <>{children}</> : <Navigate to="/login" replace state={{ from: location, notice: 'Sign in to continue.' }} />;
};

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      staleTime: 1000 * 60, // 1 minute sensible staleTime
      gcTime: 1000 * 60 * 5, // 5 minutes sensible gcTime
      retry: 1,
    },
  },
});

export const App: React.FC = () => {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <BrowserRouter>
          <Suspense fallback={<PageLoader label="Loading page" />}>
            <Routes>
              <Route path="/" element={<AppLayout />}>
                <Route index element={<Protected><TaskPoolPage /></Protected>} />
                <Route path="tasks" element={<Protected><TaskPoolPage /></Protected>} />
                <Route path="tasks/:id" element={<Protected><TaskDetailPage /></Protected>} />
                <Route path="tasks/:taskId/files/:fileId" element={<Protected><TaskFilePage /></Protected>} />
                <Route path="login" element={<LoginPage />} />
                <Route path="signup" element={<SignUpPage />} />
                <Route path="verify-email" element={<VerifyEmailPage />} />
                <Route path="set-password" element={<SetPasswordPage />} />
                <Route path="forgot-password" element={<ForgotPasswordPage />} />
                <Route path="reset-password" element={<ResetPasswordPage />} />
                <Route path="profile" element={<Protected><ProfilePage /></Protected>} />
                <Route path="my-time" element={<Protected><MyTimePage /></Protected>} />
                <Route path="admin/users" element={<Protected><AdminUsersPage /></Protected>} />
                <Route path="admin/users/:id" element={<Protected><UserDetailsPage /></Protected>} />
                <Route path="users/:id" element={<Protected><UserDetailsPage /></Protected>} />
                <Route path="admin/pending" element={<Protected><AdminPendingPage /></Protected>} />
                <Route path="*" element={<NotFoundPage />} />
              </Route>
            </Routes>
          </Suspense>
        </BrowserRouter>
      </AuthProvider>
    </QueryClientProvider>
  );
};

export default App;
