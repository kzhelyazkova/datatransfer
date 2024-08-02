package com.quickbase.datatransfer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.shell.command.annotation.CommandScan;

@SpringBootApplication
@CommandScan
public class DataTransferApplication {

	public static void main(String[] args) {
		SpringApplication.run(DataTransferApplication.class, args);
	}

}
