export type City = "HAIFA" | "TEL_AVIV" | "RAMAT_GAN" | "OTHER";
export type LocationCategory = "SPORTS_GROUND";
export type LocationSource = "OSM" | "USER";

export type FavoriteSource = "PROMISE" | "MANUAL";

export interface Place {
  id: number;
  name: string;
  latitude: number;
  longitude: number;
  category: LocationCategory;
  source: LocationSource;
  city: City;
}

export interface SlotCount {
  start: string;
  end: string;
  count: number;
  mine: boolean;
}

export interface DaySlots {
  date: string;
  slots: SlotCount[];
}

export interface Person {
  name: string;
  avatarUrl: string | null;
}

export interface SlotDetails {
  start: string;
  end: string;
  people: Person[];
}

export interface Favorite {
  id: number;
  location: Place;
  source: FavoriteSource;
  createdAt: string;
  lastActivityAt: string;
}

export interface UserProfile {
  id: number;
  name: string;
  email: string;
  avatarUrl: string;
  hasCalendarAccess?: boolean;
}

export interface AuthConfig {
  googleEnabled: boolean;
  hasClientId?: boolean;
  hasClientSecret?: boolean;
  clientIdHint?: string;
  loginUrl: string;
}
