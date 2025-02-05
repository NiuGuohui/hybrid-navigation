import { AppRegistry, ComponentProvider } from 'react-native'
import React, { useEffect, createContext } from 'react'
import { Navigator } from './Navigator'
import {
  NavigationModule,
  EventEmitter,
  EVENT_NAVIGATION,
  KEY_SCENE_ID,
  KEY_ON,
  ON_COMPONENT_APPEAR,
  ON_COMPONENT_DISAPPEAR,
} from './NavigationModule'
import { Garden } from './Garden'
import { RouteConfig, router } from './router'
import store from './store'
import { bindBarButtonItemClickEvent, removeBarButtonItemClickEvent } from './utils'
import { NavigationItem } from './typing'

export interface InjectedProps {
  navigator: Navigator
  garden: Garden
  sceneId: string
}

interface Props {
  sceneId: string
}

function getDisplayName(WrappedComponent: React.ComponentType<any>) {
  return WrappedComponent.displayName || WrappedComponent.name || 'Component'
}

export const NavigationContext = createContext<any>(null)

function withNavigator(moduleName: string) {
  return function (WrappedComponent: React.ComponentType<any>) {
    const FC = React.forwardRef((props: Props, ref: React.Ref<React.ComponentType<any>>) => {
      const { sceneId } = props

      const navigator = Navigator.of(sceneId)
      if (navigator.moduleName === undefined) {
        navigator.moduleName = moduleName
      }

      const garden = navigator.garden

      useEffect(() => {
        navigator.signalFirstRenderComplete()
        return () => {
          removeBarButtonItemClickEvent(navigator.sceneId)
          store.removeNavigator(navigator.sceneId)
          navigator.unmount()
        }
      }, [navigator])

      useEffect(() => {
        const subscription = EventEmitter.addListener(EVENT_NAVIGATION, data => {
          if (navigator.sceneId === data[KEY_SCENE_ID]) {
            if (data[KEY_ON] === ON_COMPONENT_APPEAR) {
              navigator.visibility = 'visible'
            } else if (data[KEY_ON] === ON_COMPONENT_DISAPPEAR) {
              navigator.visibility = 'invisible'
            }
          }
        })
        return () => {
          subscription.remove()
        }
      }, [navigator])

      const injected = {
        garden,
        navigator,
      }

      return (
        <NavigationContext.Provider value={navigator}>
          <WrappedComponent ref={ref} {...props} {...injected} />
        </NavigationContext.Provider>
      )
    })

    FC.displayName = `withNavigator(${getDisplayName(WrappedComponent)})`
    return FC
  }
}

export type HOC = (WrappedComponent: React.ComponentType<any>) => React.ComponentType<any>
let wrap: HOC | undefined

export class ReactRegistry {
  static registerEnded: boolean
  static startRegisterComponent(hoc?: HOC) {
    store.clear()
    wrap = hoc
    ReactRegistry.registerEnded = false
    NavigationModule.startRegisterReactComponent()
  }

  static endRegisterComponent() {
    if (ReactRegistry.registerEnded) {
      console.warn(`Please don't call ReactRegistry#endRegisterComponent multiple times.`)
      return
    }
    ReactRegistry.registerEnded = true
    NavigationModule.endRegisterReactComponent()
  }

  static registerComponent(appKey: string, getComponentFunc: ComponentProvider, routeConfig?: RouteConfig) {
    if (routeConfig) {
      router.registerRoute(appKey, routeConfig)
    }

    let WrappedComponent = getComponentFunc()
    if (wrap) {
      WrappedComponent = wrap(WrappedComponent)
    }

    // build static options
    let options: object = bindBarButtonItemClickEvent((WrappedComponent as any).navigationItem) || {}
    NavigationModule.registerReactComponent(appKey, options)

    let RootComponent = withNavigator(appKey)(WrappedComponent)
    AppRegistry.registerComponent(appKey, () => RootComponent)
  }
}

export function withNavigationItem(item: NavigationItem) {
  return function (WrappedComponent: React.ComponentType<any>): React.ComponentType<any> {
    let navigationItem = (WrappedComponent as any).navigationItem
    if (navigationItem) {
      ;(WrappedComponent as any).navigationItem = { ...navigationItem, ...item }
    } else {
      ;(WrappedComponent as any).navigationItem = item
    }
    return WrappedComponent
  }
}
