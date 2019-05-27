//
//  NTESDataCenter.h
//  Signaling_Agora
//
//  Created by Netease on 2019/4/24.
//  Copyright © 2019年 Netease. All rights reserved.
//

#import <Foundation/Foundation.h>

@class AgoraRtcEngineKit;
NS_ASSUME_NONNULL_BEGIN

@interface NTESDataCenter : NSObject

@property (nullable, nonatomic, copy) NSString *account;

@property (nullable, nonatomic, copy) NSString *token;

@property (nonatomic, assign) NSUInteger uid;

@property (nullable, nonatomic, strong) NIMSignalingChannelDetailedInfo *channelInfo;

@property (nullable, nonatomic, copy) NSString *requestId;

@property (nullable, nonatomic, copy) NSString *dstAccountId;

+ (instancetype)shareIntance;

- (NSUInteger)createRandom32BitUid;

@end

NS_ASSUME_NONNULL_END
