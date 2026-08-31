import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import type { City } from "../types";
import {
  DEFAULT_LOCALE,
  dictionaries,
  LOCALES,
  LOCALE_META,
  STORAGE_KEY,
  type Locale,
  type MessageKey,
} from "./messages";

export type { Locale, MessageKey };

type Vars = Record<string, string | number>;

function interpolate(template: string, vars?: Vars): string {
  if (!vars) {
    return template;
  }
  return template.replace(/\{(\w+)\}/g, (_, name: string) =>
    vars[name] === undefined ? `{${name}}` : String(vars[name])
  );
}

function readStoredLocale(): Locale {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored && LOCALES.includes(stored as Locale)) {
      return stored as Locale;
    }
  } catch {
    // ignore
  }
  return DEFAULT_LOCALE;
}

function applyDocumentLocale(locale: Locale): void {
  const meta = LOCALE_META[locale];
  document.documentElement.lang = meta.htmlLang;
  document.documentElement.dir = meta.dir;
}

applyDocumentLocale(readStoredLocale());

function pluralForm(locale: Locale, n: number): "one" | "two" | "few" | "many" | "other" {
  const abs = Math.abs(n);
  if (locale === "ru") {
    const mod10 = abs % 10;
    const mod100 = abs % 100;
    if (mod10 === 1 && mod100 !== 11) {
      return "one";
    }
    if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) {
      return "few";
    }
    return "many";
  }
  if (locale === "he") {
    if (abs === 1) {
      return "one";
    }
    if (abs === 2) {
      return "two";
    }
    return "other";
  }
  return abs === 1 ? "one" : "other";
}

interface I18nValue {
  locale: Locale;
  dir: "ltr" | "rtl";
  setLocale: (next: Locale) => void;
  t: (key: MessageKey, vars?: Vars) => string;
  promisedLabel: (count: number) => string;
  cityLabel: (city: City) => string;
  translateError: (message: string | null | undefined, fallback?: MessageKey) => string;
}

const I18nContext = createContext<I18nValue | null>(null);

const ERROR_KEYS: Record<string, MessageKey> = {
  slot_in_past: "errorSlotInPast",
  already_promised: "errorAlreadyPromised",
  promise_not_found: "errorPromiseNotFound",
  not_own_promise: "errorNotOwnPromise",
  invalid_slot: "errorInvalidSlot",
  not_in_favorites: "errorNotInFavorites",
  location_not_found: "errorLocationNotFound",
  unauthorized: "errorUnauthorized",
  google_not_configured: "errorGoogleNotConfigured",
  no_calendar_access: "errorNoCalendarAccess",
  calendar_api_disabled: "errorCalendarApiDisabled",
  google_missing_subject: "errorOauthMissingSubject",
  invalid_data: "errorInvalidData",
  WAKE_TIMEOUT: "errorWakeTimeout",
  "Нельзя записаться на слот в прошлом": "errorSlotInPast",
  "Вы уже обещали прийти в этот слот": "errorAlreadyPromised",
  "Обещание не найдено": "errorPromiseNotFound",
  "Можно отменить только своё обещание": "errorNotOwnPromise",
  "Слот должен начинаться на 00 или 30 минут": "errorInvalidSlot",
  "Точки нет в избранном": "errorNotInFavorites",
  "Локация не найдена": "errorLocationNotFound",
  "Нужна авторизация": "errorUnauthorized",
  "Google OAuth не настроен на сервере.": "errorGoogleNotConfigured",
  "Нет доступа к Google Calendar. Выйдите и войдите снова, разрешив календарь.": "errorNoCalendarAccess",
  "В Google Cloud не включён Calendar API для этого проекта.": "errorCalendarApiDisabled",
  "Google не вернул идентификатор пользователя": "errorOauthMissingSubject",
  "Сервер не проснулся. Обновите страницу через минуту.": "errorWakeTimeout",
  "Некорректные данные": "errorInvalidData",
};

const CITY_KEYS: Record<City, MessageKey> = {
  HAIFA: "cityHaifa",
  TEL_AVIV: "cityTelAviv",
  RAMAT_GAN: "cityRamatGan",
  OTHER: "cityOther",
};

export function I18nProvider({ children }: { children: ReactNode }) {
  const [locale, setLocaleState] = useState<Locale>(readStoredLocale);

  useEffect(() => {
    applyDocumentLocale(locale);
    try {
      localStorage.setItem(STORAGE_KEY, locale);
    } catch {
      // ignore
    }
  }, [locale]);

  const value = useMemo<I18nValue>(() => {
    const dict = dictionaries[locale];
    const t = (key: MessageKey, vars?: Vars) => interpolate(dict[key], vars);
    return {
      locale,
      dir: LOCALE_META[locale].dir,
      setLocale: setLocaleState,
      t,
      promisedLabel: (count: number) => {
        if (count === 0) {
          return t("nobodyYet");
        }
        const form = pluralForm(locale, count);
        const key = (`promised_${form}` as MessageKey);
        return t(key, { n: count });
      },
      cityLabel: (city: City) => t(CITY_KEYS[city]),
      translateError: (message, fallback = "errorGeneric") => {
        if (!message) {
          return t(fallback);
        }
        const mapped = ERROR_KEYS[message];
        if (mapped) {
          return t(mapped);
        }
        if (Object.prototype.hasOwnProperty.call(dictionaries.en, message)) {
          return t(message as MessageKey);
        }
        if (message.startsWith("HTTP_")) {
          return t("errorHttp", { status: message.slice(5) });
        }
        if (message.startsWith("calendar_error:")) {
          return t("errorCalendarGeneric", { detail: message.slice("calendar_error:".length) });
        }
        if (message.startsWith("Google Calendar: ")) {
          return t("errorCalendarGeneric", { detail: message.slice("Google Calendar: ".length) });
        }
        if (/^Ошибка \d+$/.test(message)) {
          return t("errorHttp", { status: message.replace("Ошибка ", "") });
        }
        return message;
      },
    };
  }, [locale]);

  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>;
}

export function useI18n(): I18nValue {
  const ctx = useContext(I18nContext);
  if (!ctx) {
    throw new Error("useI18n outside provider");
  }
  return ctx;
}
