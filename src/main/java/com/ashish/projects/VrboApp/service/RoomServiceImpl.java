package com.ashish.projects.VrboApp.service;

import com.ashish.projects.VrboApp.controller.HotelController;
import com.ashish.projects.VrboApp.dto.RoomDto;
import com.ashish.projects.VrboApp.entity.Hotel;
import com.ashish.projects.VrboApp.entity.Room;
import com.ashish.projects.VrboApp.entity.User;
import com.ashish.projects.VrboApp.exceptions.ResourceNotFoundException;
import com.ashish.projects.VrboApp.exceptions.UnAuthorisedException;
import com.ashish.projects.VrboApp.repository.HotelRepository;
import com.ashish.projects.VrboApp.repository.RoomRepository;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
//@Slf4j
//@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    private static final Logger log = LoggerFactory.getLogger(HotelController.class);

    private final RoomRepository roomRepository;
    private final ModelMapper modelMapper;
    private final HotelRepository hotelRepository;
    private final InventoryService inventoryService;

    public RoomServiceImpl(RoomRepository roomRepository, ModelMapper modelMapper, HotelRepository hotelRepository, InventoryService inventoryService) {
        this.roomRepository = roomRepository;
        this.modelMapper = modelMapper;
        this.hotelRepository = hotelRepository;

        this.inventoryService = inventoryService;
    }


    @Override
    public RoomDto createNewRoom(Long hotelId,RoomDto roomDto) {
        log.info("Create new room in hotel with id:{}", hotelId);
        Hotel hotel = hotelRepository
                .findById(hotelId)
                .orElseThrow(()-> new ResourceNotFoundException("hotel not found with id"+hotelId));

        User user =(User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if(user.equals(hotel.getOwner())){
            throw new UnAuthorisedException("this user does not own this hotel with id "+hotelId);
        }

        Room room = modelMapper.map(roomDto,Room.class);
        room.setHotel(hotel);
       room= roomRepository.save(room);
       //TODO: CREATE INVENTORY AS SOON AS ROOM IS CREATED ADN IF HOTEL IS ACTIVE



        if(hotel.getActive()){
             inventoryService.initializeRoomForAYear(room);
        }




       return modelMapper.map(room,RoomDto.class);
    }

    @Override
    public List<RoomDto> getAllRoomsInHotel(Long hotelId) {
        log.info("Get all rooms in hotel with id:{}", hotelId);
        Hotel hotel = hotelRepository
                .findById(hotelId)
                .orElseThrow(()-> new ResourceNotFoundException("hotel not found with id"+hotelId));

        User user =(User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if(user.equals(hotel.getOwner())){
            throw new UnAuthorisedException("this user does not own this hotel with id "+hotelId);
        }

        return hotel.getRooms()
                .stream()
                .map((element)-> modelMapper.map(element,RoomDto.class))
                .collect(Collectors.toList());
    }

    @Override
    public RoomDto getRoomById(Long roomId) {
        log.info("Geting a room with id:{}", roomId);
        Room room = roomRepository
                .findById(roomId)
                .orElseThrow(()-> new ResourceNotFoundException("room not found with id"+roomId));
        return modelMapper.map(room,RoomDto.class);


    }

    @Override
    public void deleteRoomById(Long roomId) {
        log.info("Deleting a room with id:{}", roomId);
//        Boolean exists = roomRepository.existsById(roomId);
//        if (!exists) {
//            throw new ResourceNotFoundException("room not found with id" + roomId);
//        }
        Room room = roomRepository
                .findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("room not found with id" + roomId));

        User user =(User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if(user.equals(room.getHotel().getOwner())){
            throw new UnAuthorisedException("this user does not own this room with id "+roomId);
        }
        roomRepository.deleteById(roomId);
        inventoryService.deleteAllInventories(room);
    }

}