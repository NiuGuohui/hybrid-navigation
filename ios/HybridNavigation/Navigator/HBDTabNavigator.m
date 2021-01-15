//
//  HBDTabNavigator.m
//  NavigationHybrid
//
//  Created by Listen on 2018/6/28.
//  Copyright © 2018年 Listen. All rights reserved.
//

#import "HBDTabNavigator.h"
#import "HBDTabBarController.h"
#import "HBDReactBridgeManager.h"
#import "HBDUtils.h"
#import "HBDGarden.h"
#import "GlobalStyle.h"

@implementation HBDTabNavigator

- (NSString *)name {
    return @"tabs";
}

- (NSArray<NSString *> *)supportActions {
    return @[ @"switchTab" ];
}

- (UIViewController *)createViewControllerWithLayout:(NSDictionary *)layout {
    NSDictionary *tabs = [layout objectForKey:self.name];
    NSArray *children = [tabs objectForKey:@"children"];
    if (children) {
        NSDictionary *options = [tabs objectForKey:@"options"];
        NSMutableDictionary *tabBarOptions = [@{} mutableCopy];
        NSString *tabBarModuleName = [options objectForKey:@"tabBarModuleName"];
        BOOL hasCustomTabBar = tabBarModuleName.length > 0;
        
        NSMutableArray *controllers = [[NSMutableArray alloc] initWithCapacity:4];
        for (NSDictionary *tab in children) {
            UIViewController *vc = [[HBDReactBridgeManager get] controllerWithLayout:tab];
            if (vc) {
                [controllers addObject:vc];
            }
        }
        
        if (hasCustomTabBar) {
            NSArray *tabInfos = [self tabsInfoWithChildren:controllers];
            tabBarOptions[@"tabs"] = tabInfos;
            tabBarOptions[@"tabBarModuleName"] = tabBarModuleName;
            tabBarOptions[@"sizeIndeterminate"] = @([[options objectForKey:@"sizeIndeterminate"] boolValue]);
            tabBarOptions[@"selectedIndex"] = [options objectForKey:@"selectedIndex"] ?: @(0);
            GlobalStyle *style = [HBDGarden globalStyle];
            tabBarOptions[@"tabBarItemColor"] = style.tabBarItemColorHexString;
            tabBarOptions[@"tabBarUnselectedItemColor"] = style.tabBarUnselectedItemColorHexString;
            tabBarOptions[@"badgeColor"] = style.badgeColorHexString;
        }
        
        HBDTabBarController *tabBarController = nil;
        
        if (hasCustomTabBar) {
            tabBarController = [[HBDTabBarController alloc] initWithTabBarOptions:tabBarOptions];
        } else {
            tabBarController = [[HBDTabBarController alloc] init];
        }
        
        [tabBarController setViewControllers:controllers];
        
        if (options) {
            NSNumber *selectedIndex = [options objectForKey:@"selectedIndex"];
            if (selectedIndex) {
                tabBarController.intercepted = NO;
                tabBarController.selectedIndex = [selectedIndex integerValue];
                tabBarController.intercepted = YES;
            }
        }
        
        return tabBarController;
    }
    return nil;
}

- (NSArray<NSDictionary *> *)tabsInfoWithChildren:(NSArray<UIViewController *> *)children {
    NSInteger count = children.count;
    UIViewController *vc = nil;
    NSMutableArray *tabInfos = [[NSMutableArray alloc] initWithCapacity:4];
    for (NSInteger i = 0; i < count; i ++) {
        vc = children[i];
        if ([vc isKindOfClass:[UINavigationController class]]) {
            UINavigationController *nav = (UINavigationController *)vc;
            vc = nav.childViewControllers[0];
        }
        if ([vc isKindOfClass:[HBDViewController class]]) {
            HBDViewController *hbdVC = (HBDViewController *)vc;
            NSDictionary *tabItem = [hbdVC.options objectForKey:@"tabItem"];
            if (tabItem) {
                NSDictionary *tab = @{
                      @"index": @(i),
                      @"sceneId": hbdVC.sceneId,
                      @"moduleName": hbdVC.moduleName ?: NSNull.null,
                      @"icon": [HBDUtils iconUriFromUri:tabItem[@"icon"][@"uri"]] ?: NSNull.null,
                      @"unselectedIcon": [HBDUtils iconUriFromUri:tabItem[@"unselectedIcon"][@"uri"]] ?: NSNull.null,
                      @"title": tabItem[@"title"] ?: NSNull.null
                      };
                [tabInfos addObject:tab];
            }
        }
    }
    return [tabInfos copy];
}

- (BOOL)buildRouteGraphWithController:(UIViewController *)vc root:(NSMutableArray *)root {
    if ([vc isKindOfClass:[HBDTabBarController class]]) {
        HBDTabBarController *tabBarController = (HBDTabBarController *)vc;
        NSMutableArray *children = [[NSMutableArray alloc] init];
        for (NSInteger i = 0; i < tabBarController.childViewControllers.count; i++) {
            UIViewController *child = tabBarController.childViewControllers[i];
            [[HBDReactBridgeManager get] buildRouteGraphWithController:child root:children];
        }
        [root addObject:@{
                          @"layout": self.name,
                          @"sceneId": vc.sceneId,
                          @"children": children,
                          @"mode": [vc hbd_mode],
                          @"selectedIndex": @(tabBarController.selectedIndex)
                        }];
        return YES;
    }
    return NO;
}

- (HBDViewController *)primaryViewControllerWithViewController:(UIViewController *)vc {
    if ([vc isKindOfClass:[UITabBarController class]]) {
        UITabBarController *tabBarVc = (UITabBarController *)vc;
        return [[HBDReactBridgeManager get] primaryViewControllerWithViewController:tabBarVc.selectedViewController];
    }
    return nil;
}

- (void)handleNavigationWithViewController:(UIViewController *)vc action:(NSString *)action extras:(NSDictionary *)extras resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject {
    
    UITabBarController *tabBarController = nil;
    if ([vc isKindOfClass:[UITabBarController class]]) {
        tabBarController = (UITabBarController *)vc;
    } else {
        tabBarController = vc.tabBarController;
    }
    
    if (!tabBarController) {
        resolve(@(NO));
        return;
    }
    
    if (!tabBarController.hbd_viewAppeared) {
        dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(0.5 * NSEC_PER_SEC)), dispatch_get_main_queue(), ^{
            [self handleNavigationWithViewController:vc action:action extras:extras resolver:resolve rejecter:reject];
        });
        return;
    }
    
    if ([action isEqualToString:@"switchTab"]) {
        BOOL popToRoot = [[extras objectForKey:@"popToRoot"] boolValue];
        NSInteger index = [[extras objectForKey:@"index"] integerValue];
        
        if (popToRoot) {
            UIViewController *vc = [tabBarController selectedViewController];
            UINavigationController *nav = nil;
            if ([vc isKindOfClass:[UINavigationController class]]) {
                nav = (UINavigationController *)vc;
            } else {
                nav = vc.navigationController;
            }
            
            if (nav && nav.childViewControllers.count > 1) {
                [nav popToRootViewControllerAnimated:NO];
                dispatch_async(dispatch_get_main_queue(), ^{
                    [self handleNavigationWithViewController:vc action:action extras:extras resolver:resolve rejecter:reject];
                });
                return;
            }
        }
        
        if ([tabBarController isKindOfClass:[HBDTabBarController class]]) {
            HBDTabBarController *hbdTabBarVC = (HBDTabBarController *)tabBarController;
            hbdTabBarVC.intercepted = NO;
            tabBarController.selectedIndex = index;
            hbdTabBarVC.intercepted = YES;
        } else {
            tabBarController.selectedIndex = index;
        }
    }
    
    resolve(@(YES));
}

@end
