export const extractErrorMessage = (err: unknown, defaultMessage = 'Error inesperado'): string => {
  if (typeof err === 'object' && err !== null && 'response' in err) {
    const response = (err as any).response;
    if (response?.data) {
      if (typeof response.data === 'string') {
        return response.data;
      }
      if (response.data.error) {
        return response.data.error;
      }
      if (response.data.message) {
        return response.data.message;
      }
      // If it's a field validation error map (e.g., Spring Validation)
      if (typeof response.data === 'object') {
        const values = Object.values(response.data);
        if (values.length > 0 && typeof values[0] === 'string') {
          return values[0];
        }
      }
    }
  }
  if (err instanceof Error) {
    return err.message;
  }
  return defaultMessage;
};
