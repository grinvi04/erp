import { describe, it, expect } from 'vitest'
import { sanitizeCsvValue, escapeCsvCell, toCsv } from './csv'

describe('sanitizeCsvValue — 수식 인젝션 방어 (api-standards.md §CSV export)', () => {
  it('선두 위험문자(= + - @ TAB CR)는 작은따옴표로 중화한다', () => {
    expect(sanitizeCsvValue('=1+1')).toBe("'=1+1")
    expect(sanitizeCsvValue('+1')).toBe("'+1")
    expect(sanitizeCsvValue('-1')).toBe("'-1")
    expect(sanitizeCsvValue('@SUM(A1)')).toBe("'@SUM(A1)")
    expect(sanitizeCsvValue('\tx')).toBe("'\tx")
    expect(sanitizeCsvValue('\rx')).toBe("'\rx")
    expect(sanitizeCsvValue("=cmd|'/c calc'!A1")).toBe("'=cmd|'/c calc'!A1")
  })

  it('정상 값·중간 위험문자는 변형하지 않는다', () => {
    expect(sanitizeCsvValue('정상')).toBe('정상')
    expect(sanitizeCsvValue('a=b')).toBe('a=b')
    expect(sanitizeCsvValue('100')).toBe('100')
    expect(sanitizeCsvValue('')).toBe('')
  })
})

describe('escapeCsvCell — 중화 후 RFC4180 인용 (회귀 없음)', () => {
  it('숫자·null·undefined를 안전하게 직렬화한다', () => {
    expect(escapeCsvCell(123)).toBe('123')
    expect(escapeCsvCell(null)).toBe('')
    expect(escapeCsvCell(undefined)).toBe('')
  })

  it('콤마·따옴표·줄바꿈은 기존대로 인용한다', () => {
    expect(escapeCsvCell('a,b')).toBe('"a,b"')
    expect(escapeCsvCell('a"b')).toBe('"a""b"')
    expect(escapeCsvCell('a\nb')).toBe('"a\nb"')
    expect(escapeCsvCell('plain')).toBe('plain')
  })

  it('위험문자 + 콤마는 중화 후 인용한다(둘 다 적용)', () => {
    expect(escapeCsvCell('=cmd,x')).toBe('"\'=cmd,x"')
  })
})

describe('toCsv — 헤더+행 직렬화', () => {
  it('CRLF 구분·셀별 중화/인용을 적용한다', () => {
    expect(toCsv(['코드', '이름'], [['=1', 'a,b']])).toBe('코드,이름\r\n\'=1,"a,b"')
  })
})
