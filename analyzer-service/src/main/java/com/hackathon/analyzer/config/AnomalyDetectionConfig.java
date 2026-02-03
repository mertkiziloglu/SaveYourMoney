package com.hackathon.analyzer.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@ConfigurationProperties(prefix = "anomaly.detection")
@PropertySource("classpath:anomaly-detection.properties")
@Data
public class AnomalyDetectionConfig {

    private boolean enabled = true;

    private Window window = new Window();
    private Ema ema = new Ema();
    private Threshold threshold = new Threshold();
    private Cpu cpu = new Cpu();
    private Memory memory = new Memory();
    private Pool pool = new Pool();
    private Response response = new Response();

    @Data
    public static class Window {
        private int size = 60;
    }

    @Data
    public static class Ema {
        private double alpha = 0.2;
    }

    @Data
    public static class Threshold {
        private double low = 1.5;
        private double medium = 2.0;
        private double high = 2.5;
        private double critical = 3.0;
    }

    @Data
    public static class Cpu {
        private Sustained sustained = new Sustained();

        @Data
        public static class Sustained {
            private double threshold = 80.0;
            private int count = 6;
        }
    }

    @Data
    public static class Memory {
        private Leak leak = new Leak();

        @Data
        public static class Leak {
            private Slope slope = new Slope();

            @Data
            public static class Slope {
                private double threshold = 0.05;
            }
        }
    }

    @Data
    public static class Pool {
        private Exhaustion exhaustion = new Exhaustion();

        @Data
        public static class Exhaustion {
            private double ratio = 0.9;
        }
    }

    @Data
    public static class Response {
        private Time time = new Time();

        @Data
        public static class Time {
            private Spike spike = new Spike();

            @Data
            public static class Spike {
                private double threshold = 1000.0;
            }
        }
    }
}
