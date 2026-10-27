'use client';

import React, { Component, ErrorInfo, ReactNode } from 'react';
import { AlertTriangle, RefreshCw, Home, ArrowLeft } from '@/components/icons';
import { Alert, Button, Card, Space, Typography } from 'antd';

interface Props {
 children: ReactNode;
 fallback?: ReactNode;
}

interface State {
 hasError: boolean;
 error: Error | null;
}

export class ErrorBoundary extends Component<Props, State> {
 public state: State = {
  hasError: false,
  error: null,
 };

 public static getDerivedStateFromError(error: Error): State {
  return { hasError: true, error };
 }

 public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
  console.error('Error caught by boundary:', error, errorInfo);
 }

 private handleRefresh = () => {
  window.location.reload();
 };

 private handleGoBack = () => {
  window.history.back();
 };

 private handleGoHome = () => {
  // Single-locale app (Indonesian only): the home route is always `/id`.
  window.location.href = '/id';
 };

 public render() {
  if (this.state.hasError) {
   if (this.props.fallback) {
    return this.props.fallback;
   }

   return (
    <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 16 }}>
     <Card style={{ maxWidth: 448, width: '100%', borderRadius: 16, textAlign: 'center' }}>
      <Space orientation="vertical" size={16} style={{ width: '100%' }}>
       <div style={{ height: 80, width: 80, borderRadius: 16, display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto', backgroundColor: 'var(--ant-color-error-bg)', border: '1px solid var(--ant-color-error-border)' }}>
        <AlertTriangle style={{ height: 40, width: 40, color: 'var(--ant-color-error)' }} aria-hidden="true" />
       </div>

       <Typography.Title level={2} style={{ margin: 0 }}>
        Terjadi Kesalahan
       </Typography.Title>

       <Typography.Text type="secondary">
        Maaf, terjadi kesalahan yang tidak terduga. Kami telah mencatat masalah ini dan sedang mengatasinya.
       </Typography.Text>

       {process.env.NODE_ENV === 'development' && this.state.error && (
        <Alert
         type="error"
         showIcon
         message="Detail Teknis"
         description={<span style={{ fontFamily: 'monospace', wordBreak: 'break-word' }}>{this.state.error.message}</span>}
        />
       )}

       <Space orientation="vertical" size={12} style={{ width: '100%' }}>
        <Button
         type="primary"
         block
         size="large"
         icon={<RefreshCw style={{ height: 16, width: 16 }} aria-hidden="true" />}
         onClick={this.handleRefresh}
        >
         Muat Ulang Halaman
        </Button>

        <Space size={12} style={{ width: '100%' }}>
         <Button
          block
          size="large"
          icon={<ArrowLeft style={{ height: 16, width: 16 }} aria-hidden="true" />}
          onClick={this.handleGoBack}
         >
          Kembali
         </Button>

         <Button
          block
          size="large"
          icon={<Home style={{ height: 16, width: 16 }} aria-hidden="true" />}
          onClick={this.handleGoHome}
         >
          Beranda
         </Button>
        </Space>
       </Space>

       <Typography.Text type="secondary" style={{ fontSize: 12 }}>
        Masalah berlanjut? Hubungi tim dukungan kami.
       </Typography.Text>
      </Space>
     </Card>
    </div>
   );
  }

  return this.props.children;
 }
}
