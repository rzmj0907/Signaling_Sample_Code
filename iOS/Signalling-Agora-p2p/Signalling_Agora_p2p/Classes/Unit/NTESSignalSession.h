//
//  NTESSignalSession.h
//  Signalling_Agora_p2p
//
//  Created by Netease on 2019/5/9.
//  Copyright © 2019年 Netease. All rights reserved.
//

#import <Foundation/Foundation.h>

#define kDidChannelCloseNotication @"kDidChannelCloseNotication"

@protocol NTESSignalSessionDelegate;

NS_ASSUME_NONNULL_BEGIN

typedef void(^NTESSignalBlock)(NSError *error);

@interface NTESSignalSession : NSObject<NIMSignalManagerDelegate>

@property (nonatomic, readonly) NSString *accountId;

@property (nonatomic, readonly) NSString *channelId;


@property (nonatomic, weak) id <NTESSignalSessionDelegate> delegate;


- (void)doCallWithAccountId:(NSString *)accountId
                 completion:(nullable NTESSignalBlock)completion;

- (void)doAcceptCompletion:(nullable NTESSignalBlock)completion;

- (void)doRejectCompletion:(nullable NTESSignalBlock)completion;

- (void)doCloseChannelCompletion:(nullable NTESSignalBlock)completion;

@end

@protocol NTESSignalSessionDelegate <NSObject>

- (void)didReceiveInvite;

- (void)didJoinChannel;

- (void)didCancelInvite;

- (void)didRejectInvite;

@end

NS_ASSUME_NONNULL_END
