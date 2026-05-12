import { useEffect, useState } from 'react'
import { roastApiPath, userDataApiPath } from '../../config'
import { parseErrorMessage } from './roastContent'

const initialRoastState = {
  status: 'loading',
  roast: '',
  message: '',
}

const initialUserDataState = {
  status: 'loading',
  data: null,
}

export function useRoastRequest() {
  const [state, setState] = useState(initialRoastState)

  useEffect(() => {
    const controller = new AbortController()

    async function generateRoast() {
      try {
        const response = await fetch(roastApiPath, {
          credentials: 'include',
          signal: controller.signal,
        })
        const payload = await response.json().catch(() => null)

        if (response.ok && payload?.roast) {
          setState({
            status: 'ready',
            roast: payload.roast,
            message: '',
          })
          return
        }

        if (response.status === 401) {
          setState({
            status: 'auth',
            roast: '',
            message: 'Your session is missing or expired. Authenticate with Spotify again.',
          })
          return
        }

        setState({
          status: 'error',
          roast: '',
          message: parseErrorMessage(payload),
        })
      } catch (error) {
        if (error?.name === 'AbortError') {
          return
        }

        setState({
          status: 'error',
          roast: '',
          message: 'Unable to reach the backend. Start the API and try again.',
        })
      }
    }

    generateRoast()

    return () => controller.abort()
  }, [])

  return state
}

export function useUserDataRequest() {
  const [state, setState] = useState(initialUserDataState)

  useEffect(() => {
    const controller = new AbortController()

    async function loadUserData() {
      try {
        const response = await fetch(userDataApiPath, {
          credentials: 'include',
          signal: controller.signal,
        })
        const payload = await response.json().catch(() => null)

        if (response.ok && payload) {
          setState({ status: 'ready', data: payload })
          return
        }

        setState({ status: 'error', data: null })
      } catch (error) {
        if (error?.name !== 'AbortError') {
          setState({ status: 'error', data: null })
        }
      }
    }

    loadUserData()

    return () => controller.abort()
  }, [])

  return state
}
