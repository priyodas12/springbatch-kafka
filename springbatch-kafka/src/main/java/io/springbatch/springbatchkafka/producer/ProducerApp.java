package io.springbatch.springbatchkafka.producer;

import io.springbatch.springbatchkafka.entity.Customer;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.batch.item.kafka.KafkaItemWriter;
import org.springframework.batch.item.kafka.builder.KafkaItemWriterBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@EnableBatchProcessing
@SpringBootApplication
@RequiredArgsConstructor
public class ProducerApp {

    public static void main(String[] args) {
        SpringApplication.run(ProducerApp.class,args);
    }

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final KafkaTemplate<Long,Customer> template;

    @Bean
    Job job(){
        return this.jobBuilderFactory
                .get("jobs")
                .start(step())
                .incrementer(new RunIdIncrementer())
                .build();
    }

    @Bean
    KafkaItemWriter<Long,Customer> kafkaItemWriter() {
        return new KafkaItemWriterBuilder<Long,Customer>()
                .kafkaTemplate(template)
                .itemKeyMapper(Customer::getId)
                .build();
    }

    @Bean
    Step step(){
        var id=new AtomicLong();
        var reader=new ItemReader<Customer>(){
           @Override
           public Customer read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
               if(id.incrementAndGet()<10_1000) {
                   return new Customer(id.get(),Math.random()>.5?"abc":"bca");
               }
               return null;
           }
        };
        return this.stepBuilderFactory
               .get("s1")
               .<Customer ,Customer>chunk(10)
               .reader(reader)
               .writer(kafkaItemWriter())
               .build();
    }
}
