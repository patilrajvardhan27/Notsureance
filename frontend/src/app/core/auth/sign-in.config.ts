import { EnvironmentProviders, InjectionToken, Provider } from '@angular/core';
import {
  MSAL_GUARD_CONFIG,
  MSAL_INSTANCE,
  MsalBroadcastService,
  MsalGuard,
  MsalGuardConfiguration,
  MsalService,
} from '@azure/msal-angular';
import { BrowserCacheLocation, InteractionType, PublicClientApplication } from '@azure/msal-browser';

export interface SignInConfig {
  clientId: string;
  authority: string;
  apiScopes: string[];
}

export const SIGN_IN_CONFIG = new InjectionToken<SignInConfig>('SIGN_IN_CONFIG');

// Tokens stayed in memory only; the cookie holds just the short-lived PKCE state for the redirect round trip.
export async function createMsalInstance(config: SignInConfig): Promise<PublicClientApplication> {
  const instance = new PublicClientApplication({
    auth: {
      clientId: config.clientId,
      authority: config.authority,
      redirectUri: window.location.origin,
      postLogoutRedirectUri: window.location.origin,
    },
    cache: { cacheLocation: BrowserCacheLocation.MemoryStorage, storeAuthStateInCookie: true },
  });
  await instance.initialize();
  return instance;
}

export function signInProviders(config: SignInConfig, instance: PublicClientApplication): (Provider | EnvironmentProviders)[] {
  const guardConfig: MsalGuardConfiguration = {
    interactionType: InteractionType.Redirect,
    authRequest: { scopes: config.apiScopes },
    loginFailedRoute: '/login',
  };
  return [
    { provide: SIGN_IN_CONFIG, useValue: config },
    { provide: MSAL_INSTANCE, useValue: instance },
    { provide: MSAL_GUARD_CONFIG, useValue: guardConfig },
    MsalService,
    MsalGuard,
    MsalBroadcastService,
  ];
}
