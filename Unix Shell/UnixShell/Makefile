
INCS = shelpers.hpp

OBJS = main.o shelpers.o

CXXFLAGS = --std=c++14 -O2

shell: $(OBJS)
	$(CXX) $(CXXFLAGS) -o shell $(OBJS)

main.o: main.cpp $(INCS)
	$(CXX) $(CXXFLAGS) -c $<

shelpers.o: shelpers.cpp $(INCS)
	$(CXX) $(CXXFLAGS) -c $<


