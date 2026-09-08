const UNSPLASH_PARAMS = 'auto=format&fit=crop&w=1200&q=80'

function unsplash(photoId: string): string {
  return `https://images.unsplash.com/${photoId}?${UNSPLASH_PARAMS}`
}

const GENERIC_PHOTOS = [
  unsplash('photo-1488646953014-85cb44e25828'),
  unsplash('photo-1469854523086-cc02fe5d8800'),
  unsplash('photo-1476514525535-07fb3b4ae5f1'),
  unsplash('photo-1507525428034-b723cf961d3e'),
]

const CITY_PHOTOS: Record<string, string[]> = {
  BUE: [
    unsplash('photo-1587595431973-160d0d94add1'),
    unsplash('photo-1578632767115-351597cf2477'),
    unsplash('photo-1566073771259-6a8506099945'),
    unsplash('photo-1542314831-068cd1dbfeeb'),
  ],
  PAR: [
    unsplash('photo-1502602898657-3e91760cbb34'),
    unsplash('photo-1499856871958-5b9627545d1a'),
    unsplash('photo-1549144511-f099e773c147'),
    unsplash('photo-1511739001486-6bfe10ce785f'),
  ],
  LON: [
    unsplash('photo-1513635269975-59663e0ac1ad'),
    unsplash('photo-1486299267070-83823f5448dd'),
    unsplash('photo-1529655683826-aba9b3e77383'),
    unsplash('photo-1533929736458-ca588d08c8be'),
  ],
  ROM: [
    unsplash('photo-1552832230-c0197dd311b5'),
    unsplash('photo-1529260830199-42c24126f198'),
    unsplash('photo-1515542622106-78bda8ba0e5b'),
    unsplash('photo-1582719478250-c89cae4dc85b'),
  ],
  NYC: [
    unsplash('photo-1496442226666-8d4d0e62e6e9'),
    unsplash('photo-1485871981521-5b1fd3805eee'),
    unsplash('photo-1480714378408-67cf0d13bc1b'),
    unsplash('photo-1449824913935-59a10b8d2000'),
  ],
  TYO: [
    unsplash('photo-1540959733332-eab4deabeeaf'),
    unsplash('photo-1542051841857-5f90071e7989'),
    unsplash('photo-1480796927426-f609979314bd'),
    unsplash('photo-1524413840807-0c3cb6fa808d'),
  ],
  RIO: [
    unsplash('photo-1772603503638-74ca65f96589'),
    unsplash('photo-1763110805416-22e7a6fffa58'),
    unsplash('photo-1518639192441-8fce0a366e2e'),
    unsplash('photo-1624138784614-87fd1b6528f8'),
  ],
  BCN: [
    unsplash('photo-1583422409516-2895a77efded'),
    unsplash('photo-1539037116277-4db20889f2d4'),
    unsplash('photo-1523531294919-4bcd7c65e216'),
    unsplash('photo-1558642452-9d2a7deb7f62'),
  ],
  CUN: [
    unsplash('photo-1507525428034-b723cf961d3e'),
    unsplash('photo-1473496169904-658ba7c44d8a'),
    unsplash('photo-1500375592092-40eb2168fd21'),
    unsplash('photo-1559827260-dc66d52bef19'),
  ],
  DXB: [
    unsplash('photo-1512453979798-5ea266f8880c'),
    unsplash('photo-1518684079-3c830dcef090'),
    unsplash('photo-1546412414-8035e1776c9a'),
    unsplash('photo-1564501049412-61c2a3083791'),
  ],
}

export function photosForCity(code: string): string[] {
  const photos = CITY_PHOTOS[code.trim().toUpperCase()] ?? GENERIC_PHOTOS
  return [...photos]
}

export function isUnusableHotelPhotoUrl(src: string): boolean {
  const trimmed = src.trim()
  if (!trimmed) {
    return true
  }
  try {
    const url = new URL(trimmed)
    return url.hostname === 'images.ctv.demo' || url.hostname.endsWith('.ctv.demo')
  } catch {
    return true
  }
}
