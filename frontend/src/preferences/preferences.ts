export interface UserPreferences {
  originCityCode: string
  maxBudget: string
  applyOriginOnSearch: boolean
}

export const DEFAULT_PREFERENCES: UserPreferences = {
  originCityCode: '',
  maxBudget: '',
  applyOriginOnSearch: true,
}

export function preferencesKey(userId: number): string {
  return `ctv.preferences.${userId}`
}

export function readPreferences(userId: number): UserPreferences {
  const raw = localStorage.getItem(preferencesKey(userId))
  if (!raw) {
    return { ...DEFAULT_PREFERENCES }
  }

  try {
    const parsed = JSON.parse(raw) as Partial<UserPreferences>
    return {
      originCityCode: typeof parsed.originCityCode === 'string' ? parsed.originCityCode : '',
      maxBudget: typeof parsed.maxBudget === 'string' ? parsed.maxBudget : '',
      applyOriginOnSearch:
        typeof parsed.applyOriginOnSearch === 'boolean' ? parsed.applyOriginOnSearch : true,
    }
  } catch {
    return { ...DEFAULT_PREFERENCES }
  }
}

export function writePreferences(userId: number, preferences: UserPreferences): void {
  localStorage.setItem(preferencesKey(userId), JSON.stringify(preferences))
}

export function profileCompleteness(
  user: { firstName: string; lastName: string },
  preferences: UserPreferences,
): { completed: number; total: number; percent: number } {
  const checks = [
    Boolean(user.firstName.trim() && user.lastName.trim()),
    Boolean(preferences.originCityCode),
    Boolean(preferences.maxBudget.trim()),
  ]
  const completed = checks.filter(Boolean).length
  const total = checks.length
  return {
    completed,
    total,
    percent: Math.round((completed / total) * 100),
  }
}
