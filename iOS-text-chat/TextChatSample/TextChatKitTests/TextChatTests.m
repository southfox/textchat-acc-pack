//
//  TextChatTest.m
//  TextChatSample
//
//  Created by acn on 20/04/16.
//  Copyright © 2016 Esteban Cordero. All rights reserved.
//
//
#import <XCTest/XCTest.h>
#import "TextChat.h"

@interface TextChatTest : XCTestCase

@end

@implementation TextChatTest

- (void)setUp {
    [super setUp];
    // Put setup code here. This method is called before the invocation of each test method in the class.
}

- (void)tearDown {
    // Put teardown code here. This method is called after the invocation of each test method in the class.
    [super tearDown];
}

- (void)testExample {
    // This is an example of a functional test case.
    // Use XCTAssert and related functions to verify your tests produce the correct results.
    TextChat* tc = [[TextChat alloc] init];
    tc = [tc initWithMessage:nil alias:nil senderId:nil];
    
    XCTAssertNil([tc getTextChatSignalJSONString]);
    
    tc = [tc initWithMessage:@" " alias:@" " senderId:@" "];
    
    XCTAssertNotEqual([tc getTextChatSignalJSONString], @"");
    
}

- (void)testPerformanceExample {
    // This is an example of a performance test case.
    [self measureBlock:^{
        // Put the code you want to measure the time of here.
    }];
}

@end