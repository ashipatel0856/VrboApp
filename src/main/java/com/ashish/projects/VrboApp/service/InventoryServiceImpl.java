package com.ashish.projects.VrboApp.service;
import com.ashish.projects.VrboApp.dto.HotelPriceDto;
import com.ashish.projects.VrboApp.dto.HotelSearchRequest;

import com.ashish.projects.VrboApp.entity.Inventory;
import com.ashish.projects.VrboApp.entity.Room;
import com.ashish.projects.VrboApp.repository.HotelMinPriceRepository;
import com.ashish.projects.VrboApp.repository.InventoryRepository;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private static final Logger log = LoggerFactory.getLogger(HotelServiceImpl.class);
    private final ModelMapper modelMapper;
    private HotelMinPriceRepository hotelMinPriceRepository;

    public InventoryServiceImpl(InventoryRepository inventoryRepository, ModelMapper modelMapper, HotelMinPriceRepository hotelMinPriceRepository) {
        this.inventoryRepository = inventoryRepository;
        this.modelMapper = modelMapper;
        this.hotelMinPriceRepository = hotelMinPriceRepository;
    }



    @Override
    public void initializeRoomForAYear(Room room) {
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusYears(1);
        for(; !today.isAfter(endDate); today= today.plusDays(1)) {
            Inventory inventory = Inventory.builder()
                    .hotel(room.getHotel())
                    .room(room)
                    .bookedCount(0)
                    .city(room.getHotel().getCity())
                    .date(today)
                    .price(room.getBaseprice())
                    .surgeFactor(BigDecimal.ONE)
                    .totalCount(room.getTotalCount())
                    .closed(false)
                    .build();
            inventoryRepository.save(inventory);
        }

    }

    @Override
    public void deleteAllInventories(Room room) {
        inventoryRepository.deleteByRoom(room);
    }

    @Override
    public Page<HotelPriceDto> searchHotels(HotelSearchRequest hotelSearchRequest) {
        Pageable pageable = PageRequest.of(hotelSearchRequest.getPage(), hotelSearchRequest.getSize());
        long dateCount = ChronoUnit.DAYS.between( hotelSearchRequest.getStartDate(), hotelSearchRequest.getEndDate())+1;
//business login-90days
        Page<HotelPriceDto> hotelPage =
                hotelMinPriceRepository.findHotelsWithAvailableInventory(hotelSearchRequest.getCity(),
                hotelSearchRequest.getStartDate(),
                hotelSearchRequest.getEndDate(),
                hotelSearchRequest.getRoomsCount(),
                dateCount,pageable);

        return hotelPage;
    }

}
