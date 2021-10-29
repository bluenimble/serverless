package com.bluenimble.platform.sdks.aws.examples;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;

public class SendSmS {
    public static void main(String[] args) {
        // Your Credentials
        String ACCESS_KEY = "AKIAVK6G3JHTHA7ITRBZ";
        String SECRET_KEY = "ByiPHO4kfM3FL0z/RQCWWHXP/hWQhuYIy7PSVXrQ";
        
        BasicAWSCredentials basicAwsCredentials = new BasicAWSCredentials (ACCESS_KEY, SECRET_KEY);
        AmazonSNS snsClient = AmazonSNSClient
                              .builder ()
                              .withRegion (Regions.US_EAST_1)
                              .withCredentials (new AWSStaticCredentialsProvider(basicAwsCredentials))
                              .build ();

        String message = "Bring the phone bro";
        String phoneNumber = "4084200175";  // Ex: +91XXX4374XX
        sendSMSMessage(snsClient, message, phoneNumber);
    }
    // Send SMS to a Phone Number
    public static void sendSMSMessage(AmazonSNS snsClient,
                          String message, String phoneNumber) {
    PublishResult result = snsClient.publish(new PublishRequest()
                .withMessage(message)
                .withPhoneNumber(phoneNumber));
        System.out.println(result); // Prints the message ID.
    }
}