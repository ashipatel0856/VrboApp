package com.ashish.projects.VrboApp.service;

import com.ashish.projects.VrboApp.dto.BookingDto;
import com.ashish.projects.VrboApp.dto.BookingRequest;
import com.ashish.projects.VrboApp.dto.GuestDto;
import com.ashish.projects.VrboApp.entity.*;
import com.ashish.projects.VrboApp.entity.enums.BookingStatus;
import com.ashish.projects.VrboApp.exceptions.ResourceNotFoundException;
import com.ashish.projects.VrboApp.exceptions.UnAuthorisedException;
import com.ashish.projects.VrboApp.repository.*;
import jakarta.servlet.ServletContext;
import jakarta.transaction.Transactional;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Transactional
public class BookingServiceImpl implements BookingService {

    private static final Logger log = LoggerFactory.getLogger(HotelServiceImpl.class);
    private final HotelRepository hotelRepository;
    private final ModelMapper modelMapper;
    private final GuestRepository guestRepository;
    private final ServletContext servletContext;
    private BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final InventoryRepository inventoryRepository;


    public BookingServiceImpl(BookingRepository bookingRepository, HotelRepository hotelRepository, RoomRepository roomRepository, InventoryRepository inventoryRepository, ModelMapper modelMapper, GuestRepository guestRepository, ServletContext servletContext) {
        this.bookingRepository = bookingRepository;
        this.hotelRepository = hotelRepository;
        this.roomRepository = roomRepository;
        this.inventoryRepository = inventoryRepository;
        this.modelMapper = modelMapper;
        this.guestRepository = guestRepository;
        this.servletContext = servletContext;
    }

    @Override
    @Transactional
    public BookingDto initialiseBooking(BookingRequest bookingRequest) {
        log.info("initialise booking request for hotel : {}, room : {}, date {} {}", bookingRequest.getHotelId(), bookingRequest.getRoomId(), bookingRequest.getCheckInDate(), bookingRequest.getCheckOutDate());
        Hotel hotel = hotelRepository.findById(bookingRequest
                        .getHotelId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Hotel not found with id:" + bookingRequest.getHotelId()));

        Room room = roomRepository.findById(bookingRequest
                        .getRoomId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Room not found with id" + bookingRequest.getRoomId()));

        List<Inventory> inventoryList = inventoryRepository.finalAndLockAvailableInventory(room.getId(),
                bookingRequest.getCheckInDate(),
                bookingRequest.getCheckOutDate(),
                bookingRequest.getRoomsCount()
        );

        long daysCount = ChronoUnit.DAYS.between(bookingRequest.getCheckInDate(), bookingRequest.getCheckOutDate()) + 1;
        if (inventoryList.size() != daysCount) {
            throw new ResourceNotFoundException("room is not available any room");
        }

        // reserve the room/update the booked count of inventeries

        for (Inventory inventory : inventoryList) {
            inventory.setBookedCount(inventory.getBookedCount() + bookingRequest.getRoomsCount());
        }
        inventoryRepository.saveAll(inventoryList);


        // create the booking
//        User user = new User();
//        user.setId(1L); //TODO: REMOVE DUMMY USER

        // TODO; CALCULATE DYNAMIN PRICING
        Booking booking = Booking.builder()
                .bookingStatus(BookingStatus.RESERVED)
                .hotel(hotel)
                .room(room)
                .checkInDate(bookingRequest.getCheckInDate())
                .checkOutDate(bookingRequest.getCheckOutDate())
                .user(getCurrentUser())
                .roomsCount(bookingRequest.getRoomsCount())
                .amount(BigDecimal.TEN)
                .build();

        booking = bookingRepository.save(booking);
        return modelMapper.map(booking, BookingDto.class);
    }

    @Override
    public BookingDto addGuests(long bookingId, List<GuestDto> guestDtoList) {
        log.info("Adding guests for booking with id :{}", bookingId);

        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() ->
                new ResourceNotFoundException("Booking not found with id:" + bookingId));

        User user = getCurrentUser();
        if(user.equals(booking.getUser())) {

            throw new UnAuthorisedException("Booking does not belong to this user with id"+user.getId());
        }
        if(hasBookingExpired(booking)) {
            throw new IllegalStateException("BOOKING HAS ALREADY EXPIRED");
        }

        if(booking.getBookingStatus() != BookingStatus.RESERVED) {
            throw new IllegalStateException("BOOKING IS NOT RESERVED can not add guests");
        }

        for(GuestDto guestDto : guestDtoList) {
            Guest guest = modelMapper.map(guestDto, Guest.class);
            guest.setUser(user);
            guest=guestRepository.save(guest);
            booking.getGuests().add(guest);

        }
        booking.setBookingStatus(BookingStatus.GUESTS_ADDED);
        booking = bookingRepository.save(booking);
        return modelMapper.map(booking, BookingDto.class);
    }
    public boolean hasBookingExpired(Booking booking) {
        return booking.getCreatedAt().plusMinutes(10).isBefore(LocalDateTime.now());
    }


    public User getCurrentUser() {

        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}