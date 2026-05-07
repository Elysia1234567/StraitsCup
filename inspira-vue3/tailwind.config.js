import { setupInspiraUI } from '@inspira-ui/plugins'

/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{vue,js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        primary: '#165DFF',
        qwen: '#5E6AD2',
        sidebar: '#F7F8FA',
        border: '#E5E6EB',
        text: {
          primary: '#1D2129',
          secondary: '#4E5969',
          tertiary: '#86909C',
        },
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
      },
    },
  },
  plugins: [setupInspiraUI],
}

