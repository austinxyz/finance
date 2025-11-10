// Exchange rate utilities
// Base currency is USD

const EXCHANGE_RATES = {
  'USD': 1.0,
  'CNY': 0.14,  // 1 CNY = 0.14 USD (approximately 7.14 CNY per USD)
  'EUR': 1.08,  // 1 EUR = 1.08 USD
  'GBP': 1.27,  // 1 GBP = 1.27 USD
  'JPY': 0.0067,  // 1 JPY = 0.0067 USD (approximately 150 JPY per USD)
  'HKD': 0.13,  // 1 HKD = 0.13 USD (approximately 7.8 HKD per USD)
  'AUD': 0.65,  // 1 AUD = 0.65 USD
  'CAD': 0.72,  // 1 CAD = 0.72 USD
  'SGD': 0.74,  // 1 SGD = 0.74 USD
  'KRW': 0.00075  // 1 KRW = 0.00075 USD (approximately 1330 KRW per USD)
}

/**
 * Get exchange rate for converting a currency to USD (base currency)
 * @param {string} currency - Currency code (e.g., 'CNY', 'USD', 'EUR')
 * @returns {number} Exchange rate to convert to USD
 */
export function getExchangeRate(currency) {
  if (!currency) {
    return 1.0
  }

  const rate = EXCHANGE_RATES[currency.toUpperCase()]
  if (rate === undefined) {
    console.warn(`Unknown currency: ${currency}, defaulting to rate 1.0`)
    return 1.0
  }

  return rate
}

/**
 * Convert an amount from one currency to USD (base currency)
 * @param {number} amount - Amount to convert
 * @param {string} currency - Source currency code
 * @returns {number} Amount in USD
 */
export function convertToBaseCurrency(amount, currency) {
  if (!amount || amount === 0) {
    return 0
  }

  const rate = getExchangeRate(currency)
  return amount * rate
}

/**
 * Get all supported currencies
 * @returns {Array<string>} Array of currency codes
 */
export function getSupportedCurrencies() {
  return Object.keys(EXCHANGE_RATES)
}

/**
 * Format currency display with symbol
 * @param {string} currency - Currency code
 * @returns {string} Currency symbol
 */
export function getCurrencySymbol(currency) {
  const currencyMap = {
    'CNY': '¥',
    'USD': '$',
    'EUR': '€',
    'GBP': '£',
    'JPY': '¥',
    'HKD': 'HK$',
    'AUD': 'A$',
    'CAD': 'C$',
    'SGD': 'S$',
    'KRW': '₩'
  }
  return currencyMap[currency] || currency + ' '
}
