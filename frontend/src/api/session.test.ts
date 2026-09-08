import { describe, expect, it } from 'vitest'
import { clearSession, getStoredUser, getToken, persistSession, readInitialSession } from './session'

describe('session', () => {
  it('01 - saves and reads token and user', () => {
    persistSession('abc', {
      id: 1,
      username: 'buyer',
      firstName: 'Bruno',
      lastName: 'Buyer',
      role: 'BUYER',
    })

    expect(getToken()).toBe('abc')
    expect(getStoredUser()?.username).toBe('buyer')
  })

  it('02 - clears the stored session', () => {
    persistSession('abc', {
      id: 1,
      username: 'buyer',
      firstName: 'Bruno',
      lastName: 'Buyer',
      role: 'BUYER',
    })
    clearSession()

    expect(getToken()).toBeNull()
    expect(getStoredUser()).toBeNull()
  })

  it('03 - reads the initial session from storage', () => {
    persistSession('abc', {
      id: 1,
      username: 'buyer',
      firstName: 'Bruno',
      lastName: 'Buyer',
      role: 'BUYER',
    })

    expect(readInitialSession()).toEqual({
      token: 'abc',
      user: {
        id: 1,
        username: 'buyer',
        firstName: 'Bruno',
        lastName: 'Buyer',
        role: 'BUYER',
      },
    })
  })
})
