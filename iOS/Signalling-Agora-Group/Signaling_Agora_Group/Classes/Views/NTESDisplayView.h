//
//  NTESDisplayView.h
//  Signalling_Agora_Group
//
//  Created by Netease on 2019/5/13.
//  Copyright © 2019 Netease. All rights reserved.
//

#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

@class NTESDisplayModel;

@interface NTESDisplayView : UIView

- (void)addDisplayModel:(NTESDisplayModel *)model;

- (void)removeDisplayModeWithAccount:(NSString *)account;

- (void)setDisplayModelHidden:(BOOL)hidden uid:(NSUInteger)uid;

@end


@interface NTESDisplayModel : NSObject

@property (nonatomic, copy) NSString *account;

@property (nonatomic, assign) NSUInteger uid;

@property (nonatomic, strong) UIView *view;

@property (nonatomic, strong) UILabel *info;

@end

NS_ASSUME_NONNULL_END
