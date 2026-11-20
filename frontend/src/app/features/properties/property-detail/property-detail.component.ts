import { CommonModule, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { LanguageService } from '../../../core/services/language.service';
import { TokenStorageService } from '../../../core/services/token-storage.service';
import { ViewingSlot } from '../../viewings/models/viewing-slot.model';
import { ViewingSlotService } from '../../viewings/services/viewing-slot.service';
import { ViewingService } from '../../viewings/services/viewing.service';
import {
  AmenityDto,
  LocationNode,
  PropertyResponse,
  PropertyTypeDto,
} from '../models/property.model';
import { PropertyImage, PropertyImageService } from '../services/property-image.service';
import { PropertyService } from '../services/property.service';

@Component({
  selector: 'app-property-detail',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, DatePipe, FormsModule, RouterLink, TranslocoPipe],
  template: `
    <div class="mx-auto max-w-6xl space-y-6 py-4 text-start">
      <!-- Top Breadcrumb & Owner Actions -->
      <div class="flex flex-wrap items-center justify-between gap-3">
        <a
          routerLink="/listings"
          data-testid="back-to-listings"
          class="inline-flex items-center gap-2 rounded-xl border border-slate-200 bg-white ps-3.5 pe-3.5 py-2 text-sm font-medium text-slate-700 shadow-2xs transition hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-200 dark:hover:bg-slate-800"
        >
          <svg
            class="h-4 w-4 rtl:rotate-180"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M10 19l-7-7m0 0l7-7m-7 7h18"
            />
          </svg>
          <span>{{ 'PROPERTY.DETAIL.BACK_TO_LISTINGS' | transloco }}</span>
        </a>

        @if (property() && canManageProperty()) {
          <div class="flex flex-wrap items-center gap-2">
            @if (property()!.status === 'DRAFT' || property()!.status === 'PUBLISHED') {
              <a
                [routerLink]="['/properties', property()!.id, 'edit']"
                data-testid="detail-edit-btn"
                class="inline-flex items-center gap-1.5 rounded-xl border border-slate-300 bg-white ps-3.5 pe-3.5 py-2 text-sm font-medium text-slate-700 shadow-2xs transition hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-200 dark:hover:bg-slate-800"
              >
                {{ 'MY_PROPERTIES.ACTIONS.EDIT' | transloco }}
              </a>
            }
            @if (
              property()!.status === 'DRAFT' ||
              property()!.status === 'RENTED' ||
              property()!.status === 'ARCHIVED'
            ) {
              <button
                type="button"
                (click)="publishCurrentProperty()"
                [disabled]="updatingStatus()"
                data-testid="detail-publish-btn"
                class="inline-flex cursor-pointer items-center gap-1.5 rounded-xl bg-emerald-600 ps-3.5 pe-3.5 py-2 text-sm font-semibold text-white shadow-2xs transition hover:bg-emerald-700 disabled:opacity-50 dark:hover:bg-emerald-500"
              >
                {{
                  (property()!.status === 'ARCHIVED'
                    ? 'MY_PROPERTIES.ACTIONS.REPUBLISH'
                    : 'MY_PROPERTIES.ACTIONS.PUBLISH') | transloco
                }}
              </button>
            }
            @if (property()!.status === 'PUBLISHED') {
              <a
                [routerLink]="['/viewings/availability', property()!.id]"
                data-testid="detail-availability-btn"
                class="inline-flex items-center gap-1.5 rounded-xl bg-emerald-600 ps-3.5 pe-3.5 py-2 text-sm font-semibold text-white shadow-2xs transition hover:bg-emerald-700 dark:hover:bg-emerald-500"
              >
                {{ 'VIEWINGS.MY.MANAGE_AVAILABILITY' | transloco }}
              </a>
            }
            @if (
              property()!.status === 'PUBLISHED' ||
              property()!.status === 'RENTED' ||
              property()!.status === 'SOLD'
            ) {
              <button
                type="button"
                (click)="archiveCurrentProperty()"
                [disabled]="updatingStatus()"
                data-testid="detail-archive-btn"
                class="inline-flex cursor-pointer items-center gap-1.5 rounded-xl border border-amber-300 bg-amber-50 ps-3.5 pe-3.5 py-2 text-sm font-semibold text-amber-800 shadow-2xs transition hover:bg-amber-100 disabled:opacity-50 dark:border-amber-800 dark:bg-amber-950/60 dark:text-amber-300 dark:hover:bg-amber-900/60"
              >
                {{ 'MY_PROPERTIES.ACTIONS.ARCHIVE' | transloco }}
              </button>
            }
          </div>
        }
      </div>

      @if (statusErrorKey()) {
        <div
          role="alert"
          class="rounded-xl bg-rose-50 p-4 text-sm font-medium text-rose-800 ring-1 ring-rose-200 dark:bg-rose-950/50 dark:text-rose-200 dark:ring-rose-800/80"
        >
          {{ statusErrorKey()! | transloco }}
        </div>
      }

      @if (loading()) {
        <div class="grid grid-cols-1 gap-6 lg:grid-cols-3">
          <div class="space-y-4 lg:col-span-2">
            <div class="h-80 animate-pulse rounded-2xl bg-slate-200 dark:bg-slate-800"></div>
            <div class="h-40 animate-pulse rounded-2xl bg-slate-200 dark:bg-slate-800"></div>
          </div>
          <div class="h-80 animate-pulse rounded-2xl bg-slate-200 dark:bg-slate-800"></div>
        </div>
      } @else if (error()) {
        <div
          role="alert"
          data-testid="detail-error"
          class="rounded-2xl border border-slate-200 bg-white p-10 text-center shadow-2xs dark:border-slate-800 dark:bg-slate-900"
        >
          <p class="text-base font-semibold text-slate-800 dark:text-slate-100">
            {{ 'PROPERTY.ERRORS.LOAD_FAILED' | transloco }}
          </p>
          <a
            routerLink="/listings"
            class="mt-4 inline-flex items-center rounded-xl bg-emerald-600 ps-4 pe-4 py-2 text-sm font-semibold text-white transition hover:bg-emerald-700 dark:hover:bg-emerald-500"
          >
            {{ 'PROPERTY.DETAIL.BACK_TO_LISTINGS' | transloco }}
          </a>
        </div>
      } @else if (property(); as prop) {
        <!-- Photo Gallery -->
        <section
          class="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-2xs dark:border-slate-800 dark:bg-slate-900"
          data-testid="property-gallery"
        >
          @if (images().length > 0 && activeImage(); as img) {
            <div class="relative h-80 w-full bg-slate-900 sm:h-96">
              <img
                [src]="img.url"
                [alt]="prop.title"
                class="h-full w-full object-cover"
              />
              <div class="absolute top-4 start-4 flex flex-wrap gap-2">
                <span
                  class="rounded-lg bg-emerald-600 ps-3 pe-3 py-1 text-xs font-bold uppercase tracking-wider text-white shadow-xs"
                >
                  {{
                    prop.purpose === 'SALE'
                      ? ('LISTINGS.CARD.FOR_SALE' | transloco)
                      : ('LISTINGS.CARD.FOR_RENT' | transloco)
                  }}
                </span>
                @if (propertyTypeName()) {
                  <span
                    class="rounded-lg bg-slate-900/80 ps-3 pe-3 py-1 text-xs font-semibold text-white backdrop-blur-xs"
                  >
                    {{ propertyTypeName() }}
                  </span>
                }
              </div>
              <div class="absolute top-4 end-4">
                <span
                  class="rounded-lg bg-white/95 ps-3 pe-3 py-1 text-xs font-bold text-slate-800 shadow-xs dark:bg-slate-900/90 dark:text-slate-100"
                >
                  {{ 'PROPERTY.STATUS.' + prop.status | transloco }}
                </span>
              </div>
            </div>
            @if (images().length > 1) {
              <div class="flex gap-2.5 overflow-x-auto border-t border-slate-100 p-3 dark:border-slate-800">
                @for (item of images(); track item.id; let idx = $index) {
                  <button
                    type="button"
                    (click)="selectedImageIndex.set(idx)"
                    class="relative h-16 w-24 shrink-0 cursor-pointer overflow-hidden rounded-lg border-2 transition"
                    [class.border-emerald-600]="selectedImageIndex() === idx"
                    [class.border-transparent]="selectedImageIndex() !== idx"
                  >
                    <img
                      [src]="item.thumbnailUrl || item.url"
                      [alt]="prop.title"
                      class="h-full w-full object-cover"
                    />
                  </button>
                }
              </div>
            }
          } @else {
            <div
              class="relative flex h-64 w-full flex-col items-center justify-center bg-gradient-to-br from-emerald-900 via-slate-900 to-slate-800 text-slate-300 sm:h-72"
            >
              <svg
                class="mb-2 h-14 w-14 text-emerald-400/80"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="1.5"
                  d="M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2z"
                />
              </svg>
              <span class="text-xs font-medium uppercase tracking-widest text-slate-300">
                {{ 'LISTINGS.CARD.NO_IMAGE' | transloco }}
              </span>
              <div class="absolute top-4 start-4 flex flex-wrap gap-2">
                <span
                  class="rounded-lg bg-emerald-600 ps-3 pe-3 py-1 text-xs font-bold uppercase tracking-wider text-white shadow-xs"
                >
                  {{
                    prop.purpose === 'SALE'
                      ? ('LISTINGS.CARD.FOR_SALE' | transloco)
                      : ('LISTINGS.CARD.FOR_RENT' | transloco)
                  }}
                </span>
                @if (propertyTypeName()) {
                  <span
                    class="rounded-lg bg-white/15 ps-3 pe-3 py-1 text-xs font-semibold text-white backdrop-blur-xs"
                  >
                    {{ propertyTypeName() }}
                  </span>
                }
              </div>
              <div class="absolute top-4 end-4">
                <span
                  class="rounded-lg bg-white/95 ps-3 pe-3 py-1 text-xs font-bold text-slate-800 shadow-xs dark:bg-slate-900/90 dark:text-slate-100"
                >
                  {{ 'PROPERTY.STATUS.' + prop.status | transloco }}
                </span>
              </div>
            </div>
          }
        </section>

        <!-- Main 2-Column Content Grid -->
        <div class="grid grid-cols-1 gap-6 lg:grid-cols-3">
          <!-- Left / Start 2 Columns: Overview, Specs, Description, Amenities -->
          <div
            class="space-y-6"
            [class.lg:col-span-2]="canManageProperty() || canRequestViewing()"
            [class.lg:col-span-3]="!canManageProperty() && !canRequestViewing()"
          >
            <!-- Title, Location & Price Card -->
            <section
              class="rounded-2xl border border-slate-200 bg-white p-6 shadow-2xs dark:border-slate-800 dark:bg-slate-900"
            >
              <div class="flex flex-wrap items-start justify-between gap-4">
                <div class="space-y-1.5">
                  <div class="flex items-center gap-2 text-xs font-semibold text-slate-500 dark:text-slate-400">
                    <span
                      class="rounded bg-slate-100 ps-2 pe-2 py-0.5 text-slate-700 dark:bg-slate-800 dark:text-slate-300"
                    >
                      #{{ prop.id }}
                    </span>
                    @if (locationPath()) {
                      <span class="inline-flex items-center gap-1 text-emerald-700 dark:text-emerald-400">
                        <svg
                          class="h-3.5 w-3.5 shrink-0"
                          fill="none"
                          stroke="currentColor"
                          viewBox="0 0 24 24"
                        >
                          <path
                            stroke-linecap="round"
                            stroke-linejoin="round"
                            stroke-width="2"
                            d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"
                          />
                          <path
                            stroke-linecap="round"
                            stroke-linejoin="round"
                            stroke-width="2"
                            d="M15 11a3 3 0 11-6 0 3 3 0 016 0z"
                          />
                        </svg>
                        <span>{{ locationPath() }}</span>
                      </span>
                    }
                  </div>
                  <h1
                    class="text-2xl font-extrabold tracking-tight text-slate-900 sm:text-3xl dark:text-slate-100"
                    data-testid="detail-title"
                  >
                    {{ prop.title }}
                  </h1>
                </div>

                <div class="text-end">
                  <div
                    class="flex items-baseline gap-1.5 text-2xl font-extrabold text-emerald-700 sm:text-3xl dark:text-emerald-400"
                    data-testid="detail-price"
                  >
                    <span>{{ prop.price | number: '1.0-0' }}</span>
                    <span class="text-sm font-bold text-emerald-600 dark:text-emerald-400">
                      {{ prop.currency }}
                    </span>
                  </div>
                </div>
              </div>

              <!-- Key Specs Bar -->
              <div
                class="mt-6 grid grid-cols-3 gap-4 border-t border-slate-100 pt-5 text-center sm:text-start dark:border-slate-800"
              >
                <div class="rounded-xl bg-slate-50 p-3.5 dark:bg-slate-800/70">
                  <span class="block text-xs font-medium text-slate-500 dark:text-slate-400">
                    {{ 'PROPERTY.FORM.BEDROOMS_LABEL' | transloco }}
                  </span>
                  <span class="mt-0.5 block text-lg font-extrabold text-slate-900 dark:text-slate-100">
                    {{ prop.bedrooms }} {{ 'LISTINGS.CARD.BEDS' | transloco }}
                  </span>
                </div>
                <div class="rounded-xl bg-slate-50 p-3.5 dark:bg-slate-800/70">
                  <span class="block text-xs font-medium text-slate-500 dark:text-slate-400">
                    {{ 'PROPERTY.FORM.BATHROOMS_LABEL' | transloco }}
                  </span>
                  <span class="mt-0.5 block text-lg font-extrabold text-slate-900 dark:text-slate-100">
                    {{ prop.bathrooms }} {{ 'LISTINGS.CARD.BATHS' | transloco }}
                  </span>
                </div>
                <div class="rounded-xl bg-slate-50 p-3.5 dark:bg-slate-800/70">
                  <span class="block text-xs font-medium text-slate-500 dark:text-slate-400">
                    {{ 'PROPERTY.FORM.AREA_LABEL' | transloco }}
                  </span>
                  <span class="mt-0.5 block text-lg font-extrabold text-slate-900 dark:text-slate-100">
                    {{ prop.areaSqm }} {{ 'LISTINGS.CARD.SQM' | transloco }}
                  </span>
                </div>
              </div>
            </section>

            <!-- Description Card -->
            <section
              class="rounded-2xl border border-slate-200 bg-white p-6 shadow-2xs dark:border-slate-800 dark:bg-slate-900"
            >
              <h2 class="text-lg font-bold text-slate-900 dark:text-slate-100">
                {{ 'PROPERTY.FORM.DESCRIPTION_LABEL' | transloco }}
              </h2>
              <p class="mt-3 whitespace-pre-line text-sm leading-relaxed text-slate-600 dark:text-slate-300">
                {{ prop.description || ('PROPERTY.DETAIL.NO_DESCRIPTION' | transloco) }}
              </p>
            </section>

            <!-- Amenities Card -->
            <section
              class="rounded-2xl border border-slate-200 bg-white p-6 shadow-2xs dark:border-slate-800 dark:bg-slate-900"
            >
              <h2 class="text-lg font-bold text-slate-900 dark:text-slate-100">
                {{ 'PROPERTY.FORM.SECTION_AMENITIES' | transloco }}
              </h2>
              @if (propertyAmenities().length === 0) {
                <p class="mt-3 text-sm text-slate-500 dark:text-slate-400">
                  {{ 'PROPERTY.DETAIL.NO_AMENITIES' | transloco }}
                </p>
              } @else {
                <div class="mt-4 flex flex-wrap gap-2.5">
                  @for (am of propertyAmenities(); track am.id) {
                    <span
                      class="inline-flex items-center gap-2 rounded-xl border border-emerald-200 bg-emerald-50/70 ps-3.5 pe-3.5 py-2 text-xs font-semibold text-emerald-800 dark:border-emerald-800 dark:bg-emerald-950/60 dark:text-emerald-300"
                    >
                      <svg
                        class="h-4 w-4 text-emerald-600 dark:text-emerald-400"
                        fill="none"
                        viewBox="0 0 24 24"
                        stroke="currentColor"
                      >
                        <path
                          stroke-linecap="round"
                          stroke-linejoin="round"
                          stroke-width="2"
                          d="M5 13l4 4L19 7"
                        />
                      </svg>
                      <span>{{ isRtl() ? am.nameAr : am.nameEn }}</span>
                    </span>
                  }
                </div>
              }
            </section>
          </div>

          <!-- Right / End Sidebar Column: Owner Slots Panel OR Viewing Booking Widget -->
          @if (canManageProperty()) {
            <aside class="space-y-6 lg:col-span-1" data-testid="owner-availability-panel">
              <div
                class="rounded-2xl border border-slate-200 bg-white p-6 shadow-2xs dark:border-slate-800 dark:bg-slate-900"
              >
                <h2 class="text-lg font-bold text-slate-900 dark:text-slate-100">
                  {{ 'VIEWINGS.AVAILABILITY.SLOTS_HEADING' | transloco }}
                </h2>
                <p class="mt-1 text-xs text-slate-500 dark:text-slate-400">
                  {{ 'PROPERTY.DETAIL.OWNER_SLOTS_SUBTITLE' | transloco }}
                </p>

                @if (slots().length === 0) {
                  <div
                    class="mt-4 rounded-xl bg-slate-50 p-4 text-center text-xs text-slate-500 dark:bg-slate-800/70 dark:text-slate-400"
                  >
                    {{ 'VIEWINGS.AVAILABILITY.EMPTY_SLOTS' | transloco }}
                  </div>
                } @else {
                  <ul class="mt-4 space-y-2">
                    @for (slot of slots(); track slot.id) {
                      <li
                        class="rounded-xl border border-slate-200 bg-slate-50/70 p-3 text-xs font-semibold text-slate-800 dark:border-slate-700 dark:bg-slate-800/70 dark:text-slate-200"
                      >
                        {{ slot.startTime | date: 'mediumDate' }} •
                        {{ slot.startTime | date: 'shortTime' }} –
                        {{ slot.endTime | date: 'shortTime' }}
                      </li>
                    }
                  </ul>
                }

                @if (prop.status === 'PUBLISHED') {
                  <a
                    [routerLink]="['/viewings/availability', prop.id]"
                    data-testid="owner-manage-availability-link"
                    class="mt-4 inline-flex w-full items-center justify-center rounded-xl bg-emerald-600 py-2.5 ps-4 pe-4 text-sm font-semibold text-white shadow-2xs transition hover:bg-emerald-700 dark:hover:bg-emerald-500"
                  >
                    {{ 'VIEWINGS.MY.MANAGE_AVAILABILITY' | transloco }}
                  </a>
                }

                <div class="mt-4 border-t border-slate-100 pt-4 text-center dark:border-slate-800">
                  <a
                    routerLink="/viewings"
                    class="text-xs font-semibold text-emerald-700 hover:text-emerald-800 hover:underline dark:text-emerald-400 dark:hover:text-emerald-300"
                  >
                    {{ 'NAV.VIEWINGS' | transloco }}
                  </a>
                </div>
              </div>
            </aside>
          } @else if (canRequestViewing()) {
            <aside class="space-y-6 lg:col-span-1" data-testid="request-viewing-panel">
              <div
                class="rounded-2xl border border-slate-200 bg-white p-6 shadow-2xs dark:border-slate-800 dark:bg-slate-900"
              >
                <h2 class="text-lg font-bold text-slate-900 dark:text-slate-100">
                  {{ 'VIEWINGS.REQUEST.TITLE' | transloco }}
                </h2>
                <p class="mt-1 text-xs text-slate-500 dark:text-slate-400">
                  {{ 'VIEWINGS.REQUEST.SUBTITLE' | transloco }}
                </p>

                @if (bookingSuccess()) {
                  <div
                    role="status"
                    data-testid="detail-booking-success"
                    class="mt-4 rounded-xl bg-emerald-50 p-3.5 text-xs font-medium text-emerald-800 ring-1 ring-emerald-200 dark:bg-emerald-950/50 dark:text-emerald-200 dark:ring-emerald-800/80"
                  >
                    {{ 'VIEWINGS.REQUEST.SUCCESS' | transloco }}
                    <a
                      routerLink="/viewings"
                      class="mt-2 block font-bold text-emerald-700 underline dark:text-emerald-300"
                    >
                      {{ 'NAV.VIEWINGS' | transloco }}
                    </a>
                  </div>
                }

                @if (bookingErrorKey()) {
                  <div
                    role="alert"
                    data-testid="detail-booking-error"
                    class="mt-4 rounded-xl bg-rose-50 p-3.5 text-xs font-medium text-rose-800 ring-1 ring-rose-200 dark:bg-rose-950/50 dark:text-rose-200 dark:ring-rose-800/80"
                  >
                    {{ bookingErrorKey()! | transloco }}
                  </div>
                }

                @if (slots().length === 0) {
                  <div
                    class="mt-4 rounded-xl bg-slate-50 p-4 text-center text-xs text-slate-500 dark:bg-slate-800/70 dark:text-slate-400"
                  >
                    {{ 'VIEWINGS.REQUEST.EMPTY_SLOTS' | transloco }}
                  </div>
                } @else {
                  <div class="mt-4 space-y-2.5" role="radiogroup">
                    @for (slot of slots(); track slot.id) {
                      <label
                        class="flex cursor-pointer items-center gap-3 rounded-xl border p-3 text-xs transition"
                        [class]="
                          selectedSlotId() === slot.id
                            ? 'border-emerald-600 bg-emerald-50 dark:border-emerald-500 dark:bg-emerald-950/50'
                            : 'border-slate-200 hover:bg-slate-50 dark:border-slate-700 dark:hover:bg-slate-800'
                        "
                      >
                        <input
                          type="radio"
                          name="detailViewingSlot"
                          [value]="slot.id"
                          [checked]="selectedSlotId() === slot.id"
                          (change)="selectedSlotId.set(slot.id)"
                          class="h-4 w-4 text-emerald-600 accent-emerald-600 focus:ring-emerald-500"
                        />
                        <span class="font-semibold text-slate-800 dark:text-slate-200">
                          {{ slot.startTime | date: 'mediumDate' }} •
                          {{ slot.startTime | date: 'shortTime' }} –
                          {{ slot.endTime | date: 'shortTime' }}
                        </span>
                      </label>
                    }
                  </div>

                  <div class="mt-4">
                    <label class="block text-xs font-semibold text-slate-700 dark:text-slate-300">
                      {{ 'VIEWINGS.REQUEST.NOTES_LABEL' | transloco }}
                    </label>
                    <textarea
                      rows="2"
                      [ngModel]="bookingNotes()"
                      (ngModelChange)="bookingNotes.set($event)"
                      [placeholder]="'VIEWINGS.REQUEST.NOTES_PLACEHOLDER' | transloco"
                      class="mt-1 w-full rounded-xl border border-slate-200 bg-slate-50 ps-3 pe-3 py-2 text-xs text-slate-900 placeholder:text-slate-400 focus:border-emerald-600 focus:bg-white focus:outline-hidden dark:border-slate-700 dark:bg-slate-800 dark:text-slate-100 dark:placeholder:text-slate-500 dark:focus:bg-slate-800"
                    ></textarea>
                  </div>

                  <button
                    type="button"
                    (click)="bookViewing()"
                    [disabled]="submittingBooking() || !selectedSlotId()"
                    data-testid="detail-book-btn"
                    class="mt-4 w-full cursor-pointer rounded-xl bg-emerald-600 py-2.5 ps-4 pe-4 text-sm font-semibold text-white shadow-2xs transition hover:bg-emerald-700 disabled:cursor-not-allowed disabled:opacity-50 dark:hover:bg-emerald-500"
                  >
                    {{ 'VIEWINGS.REQUEST.SUBMIT' | transloco }}
                  </button>
                }

                <div class="mt-4 border-t border-slate-100 pt-4 text-center dark:border-slate-800">
                  <a
                    [routerLink]="['/viewings/request', prop.id]"
                    class="text-xs font-semibold text-emerald-700 hover:text-emerald-800 hover:underline dark:text-emerald-400 dark:hover:text-emerald-300"
                  >
                    {{ 'PROPERTY.DETAIL.OPEN_FULL_BOOKING' | transloco }}
                  </a>
                </div>
              </div>
            </aside>
          }
        </div>
      }
    </div>
  `,
})
export class PropertyDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly propertyService = inject(PropertyService);
  private readonly imageService = inject(PropertyImageService);
  private readonly slotService = inject(ViewingSlotService);
  private readonly viewingService = inject(ViewingService);
  private readonly tokenStorage = inject(TokenStorageService);
  private readonly languageService = inject(LanguageService);

  readonly property = signal<PropertyResponse | null>(null);
  readonly images = signal<PropertyImage[]>([]);
  readonly selectedImageIndex = signal(0);
  readonly slots = signal<ViewingSlot[]>([]);
  readonly locations = signal<LocationNode[]>([]);
  readonly propertyTypes = signal<PropertyTypeDto[]>([]);
  readonly amenities = signal<AmenityDto[]>([]);

  readonly loading = signal(true);
  readonly error = signal(false);
  readonly updatingStatus = signal(false);
  readonly statusErrorKey = signal<string | null>(null);

  readonly selectedSlotId = signal<number | null>(null);
  readonly bookingNotes = signal('');
  readonly submittingBooking = signal(false);
  readonly bookingSuccess = signal(false);
  readonly bookingErrorKey = signal<string | null>(null);

  readonly isRtl = computed(() => this.languageService.currentLang() === 'ar');

  readonly activeImage = computed(() => {
    const list = this.images();
    const idx = this.selectedImageIndex();
    return list[idx] ?? list[0] ?? null;
  });

  readonly canManageProperty = computed(() => {
    const prop = this.property();
    if (!prop || !this.tokenStorage.isAuthenticated()) return false;
    const userId = this.tokenStorage.currentUserId();
    return (
      userId !== null &&
      (userId === prop.ownerId || (prop.agentId != null && userId === prop.agentId))
    );
  });

  readonly canRequestViewing = computed(() => {
    if (!this.tokenStorage.isAuthenticated()) return true;
    if (this.canManageProperty()) return false;
    return this.tokenStorage.hasRole('BUYER_TENANT', 'OWNER', 'AGENT');
  });

  readonly propertyTypeName = computed(() => {
    const prop = this.property();
    if (!prop) return '';
    const found = this.propertyTypes().find((t) => t.id === prop.propertyTypeId);
    if (!found) return '';
    return this.isRtl() ? found.nameAr : found.nameEn;
  });

  readonly locationPath = computed(() => {
    const prop = this.property();
    const all = this.locations();
    if (!prop || all.length === 0) return '';
    const chain: string[] = [];
    let current = all.find((l) => l.id === prop.locationId);
    let safety = 0;
    while (current && safety < 6) {
      chain.unshift(this.isRtl() ? current.nameAr : current.nameEn);
      current = current.parentId ? all.find((l) => l.id === current!.parentId) : undefined;
      safety++;
    }
    return chain.join(' › ');
  });

  readonly propertyAmenities = computed(() => {
    const prop = this.property();
    const all = this.amenities();
    if (!prop || !prop.amenityIds) return [];
    return all.filter((a) => prop.amenityIds.includes(a.id));
  });

  ngOnInit(): void {
    this.propertyService.getLocations().subscribe((locs) => this.locations.set(locs));
    this.propertyService.getPropertyTypes().subscribe((types) => this.propertyTypes.set(types));
    this.propertyService.getAmenities().subscribe((ams) => this.amenities.set(ams));

    const idParam = this.route.snapshot.paramMap.get('id');
    const id = idParam ? Number(idParam) : NaN;
    if (!Number.isInteger(id) || id <= 0) {
      this.loading.set(false);
      this.error.set(true);
      return;
    }
    this.loadPropertyDetails(id);
  }

  loadPropertyDetails(id: number): void {
    this.loading.set(true);
    this.error.set(false);

    this.propertyService.getProperty(id).subscribe({
      next: (prop) => {
        this.property.set(prop);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set(true);
      },
    });

    this.imageService.list(id).subscribe({
      next: (imgs) => {
        this.images.set(imgs);
        this.selectedImageIndex.set(0);
      },
      error: () => this.images.set([]),
    });

    this.slotService.getSlots(id).subscribe({
      next: (loaded) => {
        this.slots.set(loaded);
        if (loaded.length > 0) {
          this.selectedSlotId.set(loaded[0].id);
        }
      },
      error: () => this.slots.set([]),
    });
  }

  publishCurrentProperty(): void {
    const prop = this.property();
    if (!prop || this.updatingStatus()) return;
    this.updatingStatus.set(true);
    this.statusErrorKey.set(null);
    this.propertyService.publishProperty(prop.id).subscribe({
      next: (updated) => {
        this.property.set({ ...prop, status: updated.status });
        this.updatingStatus.set(false);
      },
      error: () => {
        this.statusErrorKey.set('MY_PROPERTIES.ERRORS.ACTION_FAILED');
        this.updatingStatus.set(false);
      },
    });
  }

  archiveCurrentProperty(): void {
    const prop = this.property();
    if (!prop || this.updatingStatus()) return;
    this.updatingStatus.set(true);
    this.statusErrorKey.set(null);
    this.propertyService.archiveProperty(prop.id).subscribe({
      next: (updated) => {
        this.property.set({ ...prop, status: updated.status });
        this.updatingStatus.set(false);
      },
      error: () => {
        this.statusErrorKey.set('MY_PROPERTIES.ERRORS.ACTION_FAILED');
        this.updatingStatus.set(false);
      },
    });
  }

  bookViewing(): void {
    const slotId = this.selectedSlotId();
    const prop = this.property();
    if (!slotId || !prop) return;

    if (!this.tokenStorage.isAuthenticated()) {
      this.router.navigate(['/auth/sign-in'], {
        queryParams: { returnUrl: `/listings/${prop.id}` },
      });
      return;
    }

    this.submittingBooking.set(true);
    this.bookingSuccess.set(false);
    this.bookingErrorKey.set(null);

    const notes = this.bookingNotes().trim();
    const payload = notes ? { slotId, notes } : { slotId };

    this.viewingService.requestViewing(payload).subscribe({
      next: () => {
        this.submittingBooking.set(false);
        this.bookingSuccess.set(true);
        this.bookingNotes.set('');
      },
      error: (err: HttpErrorResponse) => {
        this.submittingBooking.set(false);
        if (err.status === 409) {
          this.bookingErrorKey.set('VIEWINGS.REQUEST.ERRORS.CONFLICT_409');
        } else if (err.status === 403) {
          this.bookingErrorKey.set('VIEWINGS.REQUEST.ERRORS.FORBIDDEN_403');
        } else if (err.status === 401) {
          this.bookingErrorKey.set('VIEWINGS.REQUEST.ERRORS.UNAUTHORIZED_401');
        } else {
          this.bookingErrorKey.set('VIEWINGS.REQUEST.ERRORS.GENERIC');
        }
      },
    });
  }
}
