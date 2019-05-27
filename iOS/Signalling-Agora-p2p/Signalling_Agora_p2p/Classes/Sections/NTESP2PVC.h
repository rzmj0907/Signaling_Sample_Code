//
//  NTESP2PVC.h
//  Signalling_Agora_p2p
//
//  Created by Netease on 2019/4/26.
//  Copyright © 2019年 Netease. All rights reserved.
//

#import <UIKit/UIKit.h>

@class NTESSignalSession;
NS_ASSUME_NONNULL_BEGIN

@interface NTESP2PVC : UIViewController

@property (nonatomic, weak) NTESSignalSession *session;

@end

NS_ASSUME_NONNULL_END
