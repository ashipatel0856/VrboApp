package com.ashish.projects.VrboApp.service;

import com.ashish.projects.VrboApp.dto.HotelDto;
import com.ashish.projects.VrboApp.dto.HotelInfoDto;
import com.ashish.projects.VrboApp.dto.RoomDto;
import com.ashish.projects.VrboApp.entity.Hotel;
import com.ashish.projects.VrboApp.entity.Room;
import com.ashish.projects.VrboApp.entity.User;
import com.ashish.projects.VrboApp.exceptions.ResourceNotFoundException;
import com.ashish.projects.VrboApp.exceptions.UnAuthorisedException;
import com.ashish.projects.VrboApp.repository.HotelRepository;

import com.ashish.projects.VrboApp.repository.InventoryRepository;
import com.ashish.projects.VrboApp.repository.RoomRepository;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;


@Service
public class HotelServiceImpl implements HotelService {
    private final HotelRepository hotelRepository;
   private final ModelMapper modelMapper;
   private final InventoryService inventoryService;
   private final RoomRepository roomRepository;

    private static final Logger log = LoggerFactory.getLogger(HotelServiceImpl.class);



    public HotelServiceImpl(HotelRepository hotelRepository, ModelMapper modelMapper, InventoryRepository inventoryRepository, InventoryService inventoryService, InventoryServiceImpl inventoryServiceImpl, RoomRepository roomRepository) {
        this.hotelRepository = hotelRepository;
        this.modelMapper = modelMapper;
        this.inventoryService = inventoryService;
        this.roomRepository = roomRepository;
    }
    @Override
    public HotelDto createNewHotel(HotelDto hotelDto) {
        log.info("Creating a new hotel with name: {}", hotelDto.getName());

        Hotel hotel = modelMapper.map(hotelDto, Hotel.class);
        hotel.setActive(false);

        User user =(User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        hotel = hotelRepository.save(hotel);
        log.info("Created a new hotel with ID: {}", hotelDto.getId());
        return modelMapper.map(hotel, HotelDto.class);
    }

    @Override
    public HotelDto getHotelById(Long id) {
        log.info("getting the hotel wiht id: {}", id);
        Hotel hotel = hotelRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id" + id));



        User user =(User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if(user.equals(hotel.getOwner())){
            throw new UnAuthorisedException("this user does not own this hotel with id "+id);
        }
        return modelMapper.map(hotel, HotelDto.class);
    }

    @Override
    public HotelDto updateHotelById(Long id, HotelDto hotelDto) {
        log.info("updating the hotel with id: {}",id);
        Hotel hotel = hotelRepository
                .findById(id)
                .orElseThrow(()->new ResourceNotFoundException("hotel not found with id"));
        modelMapper.map(hotelDto,hotel);
        hotel.setId(id);
        hotel = hotelRepository.save(hotel);
        return modelMapper.map(hotel, HotelDto.class);
    }

    @Override
    @Transactional
    public Boolean deleteHotelById(Long id) {
//        boolean exists = hotelRepository.existsById(id);
//        if (!exists) throw new ResourceNotFoundException("Hotel not found with id" + id);
//        hotelRepository.deleteById(id);
//        return  exists;

        Hotel hotel = hotelRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id" + id));


        User user =(User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if(user.equals(hotel.getOwner())){
            throw new UnAuthorisedException("this user does not own this hotel with id "+id);
        }
        hotelRepository.deleteById(id);
//        TODO: delete the future inventories for the hotel
        for (Room room : hotel.getRooms()) {
            inventoryService.deleteAllInventories(room);
            roomRepository.deleteById(room.getId());
        }
        return true;

    }

    @Override
    @Transactional
    public void activateHotel(Long hotelId) {
        log.info("activating the hotel with id:{}", hotelId);
        Hotel hotel = hotelRepository
                .findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id" + hotelId));


        User user =(User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if(user.equals(hotel.getOwner())){
            throw new UnAuthorisedException("this user does not own this hotel with id "+hotelId);
        }
        hotel.setActive(true);

        //assuming only do it one

        for(Room room: hotel.getRooms()){
            inventoryService.initializeRoomForAYear(room);
        }
        //TODO: CREATE INVENTORY FOR ALL THE ROOMS FOR THIS HOTEL
    }

    @Override
    public List<HotelDto> getAllHotels() {
        log.info("getting all the hotels");
        List<Hotel> hotels = hotelRepository.findAll();
//        return modelMapper.map(hotels,hotel.class);
        return hotels
                .stream()
                .map(hotel -> modelMapper.map(hotel, HotelDto.class))
                .collect(Collectors.toList());

    }

    @Override
    public HotelInfoDto getHotelInfoById(Long hotelId) {

        Hotel hotel = hotelRepository
                .findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id" + hotelId));
        List<RoomDto> rooms = hotel.getRooms()
                .stream()
                .map((element)-> modelMapper.map(element,RoomDto.class))
                .toList();

        return new HotelInfoDto(modelMapper.map(hotel,HotelDto.class),rooms);
    }
}
