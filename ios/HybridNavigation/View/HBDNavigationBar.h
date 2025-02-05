//
//  HBDNavigationBar.h
//  HybridNavigation
//
//  Created by Listen on 2018/3/6.
//  Copyright © 2018年 Listen. All rights reserved.
//

#import <UIKit/UIKit.h>

@interface HBDNavigationBar : UINavigationBar

@property(nonatomic, strong, readonly) UIImageView *fakeShadowView;
@property(nonatomic, strong, readonly) UIView *fakeBackgroundView;
@property(nonatomic, strong, readonly) UILabel *backButtonLabel;

@end

@interface UILabel (NavigationBarTransition)

@property(nonatomic, strong) UIColor *hbd_specifiedTextColor;

@end
