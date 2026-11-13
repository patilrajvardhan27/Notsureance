import { bootstrapApplication } from '@angular/platform-browser';
import { AppComponent } from './app/app.component';
import { appConfig } from './app/app.config';
import { SignInConfig, createMsalInstance, signInProviders } from './app/core/auth/sign-in.config';

async function start(): Promise<void> {
  const response = await fetch('/api/public/sign-in-config');
  const config = (await response.json()) as SignInConfig;
  const msal = await createMsalInstance(config);
  await bootstrapApplication(AppComponent, {
    providers: [...appConfig.providers, ...signInProviders(config, msal)],
  });
}

start().catch((err) => console.error(err));
