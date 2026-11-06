import { animate, group, query, stagger, style, transition, trigger } from '@angular/animations';

const easeOut = 'cubic-bezier(0.22, 1, 0.36, 1)';

export const routeTransition = trigger('routeTransition', [
  transition('* <=> *', [
    query(':enter', [style({ opacity: 0, transform: 'translateY(12px)' })], { optional: true }),
    group([
      query(':leave', [animate('150ms ease-in', style({ opacity: 0 }))], { optional: true }),
      query(':enter', [animate(`420ms 80ms ${easeOut}`, style({ opacity: 1, transform: 'none' }))], { optional: true }),
    ]),
  ]),
]);

export const listStagger = trigger('listStagger', [
  transition('* => *', [
    query(
      ':enter',
      [
        style({ opacity: 0, transform: 'translateY(10px)' }),
        stagger(45, [animate(`380ms ${easeOut}`, style({ opacity: 1, transform: 'none' }))]),
      ],
      { optional: true },
    ),
  ]),
]);

export const fadeSlide = trigger('fadeSlide', [
  transition(':enter', [
    style({ opacity: 0, transform: 'translateX(24px)' }),
    animate(`360ms ${easeOut}`, style({ opacity: 1, transform: 'none' })),
  ]),
  transition(':leave', [animate('160ms ease-in', style({ opacity: 0, transform: 'translateX(-24px)' }))]),
]);

export const popIn = trigger('popIn', [
  transition(':enter', [
    style({ opacity: 0, transform: 'scale(0.92)' }),
    animate(`300ms ${easeOut}`, style({ opacity: 1, transform: 'none' })),
  ]),
  transition(':leave', [animate('150ms ease-in', style({ opacity: 0, transform: 'scale(0.96)' }))]),
]);

export const expand = trigger('expand', [
  transition(':enter', [
    style({ height: 0, opacity: 0, overflow: 'hidden' }),
    animate(`320ms ${easeOut}`, style({ height: '*', opacity: 1 })),
  ]),
  transition(':leave', [
    style({ overflow: 'hidden' }),
    animate(`200ms ease-in`, style({ height: 0, opacity: 0 })),
  ]),
]);
