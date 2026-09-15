import React from 'react';

class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null, errorInfo: null };
  }

  static getDerivedStateFromError() {
    // Update state so the next render will show the fallback UI
    return { hasError: true };
  }

  componentDidCatch(error, errorInfo) {
    // Log error details
    console.error('Error Boundary caught an error:', error, errorInfo);
    
    this.setState({
      error: error,
      errorInfo: errorInfo
    });

    // You can also log the error to an error reporting service here
    // logErrorToService(error, errorInfo);
  }

  handleReload = () => {
    window.location.reload();
  };

  render() {
    if (this.state.hasError) {
      const { language = 'GE' } = this.props;
      
      return (
        <div className="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
          <div className="max-w-md w-full space-y-8">
            <div className="text-center">
              <div className="mx-auto h-12 w-12 text-red-500">
                <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L3.732 16.5c-.77.833.192 2.5 1.732 2.5z" />
                </svg>
              </div>
              <h2 className="mt-6 text-3xl font-extrabold text-gray-900 font-bpg-nino">
                {language === 'GE' ? 'რაღაც არასწორად მოხდა' : 'Something went wrong'}
              </h2>
              <p className="mt-2 text-sm text-gray-600 font-bpg-nino">
                {language === 'GE' 
                  ? 'აპლიკაციაში მოულოდნელი შეცდომა მოხდა. გთხოვთ, სცადოთ ხელახლა.'
                  : 'An unexpected error occurred in the application. Please try again.'
                }
              </p>
            </div>
            
            <div className="space-y-4">
              <button
                onClick={this.handleReload}
                className="group relative w-full flex justify-center py-2 px-4 border border-transparent text-sm font-medium rounded-md text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 font-bpg-nino"
              >
                {language === 'GE' ? 'გვერდის განახლება' : 'Reload Page'}
              </button>
              
              <button
                onClick={() => this.setState({ hasError: false, error: null, errorInfo: null })}
                className="group relative w-full flex justify-center py-2 px-4 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 font-bpg-nino"
              >
                {language === 'GE' ? 'ხელახლა ცდა' : 'Try Again'}
              </button>
            </div>

            {import.meta.env.DEV && this.state.error && (
              <details className="mt-4 p-4 bg-red-50 border border-red-200 rounded-md">
                <summary className="text-sm font-medium text-red-800 cursor-pointer">
                  Error Details (Development Only)
                </summary>
                <div className="mt-2 text-xs text-red-700 whitespace-pre-wrap">
                  {this.state.error && this.state.error.toString()}
                  <br />
                  {this.state.errorInfo.componentStack}
                </div>
              </details>
            )}
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;